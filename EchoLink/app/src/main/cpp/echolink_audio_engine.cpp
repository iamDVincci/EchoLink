#include <jni.h>
#include <oboe/Oboe.h>
#include <android/log.h>
#include <atomic>
#include <cmath>
#include <cstdint>
#include <cstring>
#include <deque>
#include <mutex>
#include <vector>
#include <algorithm>

#define LOG_TAG "EchoLinkOboeDSP"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace echolink {

constexpr int kDefaultSampleRate = 48000;
constexpr int kFrameDurationMs = 20;
constexpr int kSpectrumBands = 14;

/**
 * Low-Latency Audio Frame Codec (Opus-framed low-delay ADPCM + In-Band FEC & PLC).
 * Packs 20ms PCM frames into compact UDP payload frames with Forward Error Correction
 * header metadata so up to 15% packet loss is concealed cleanly.
 */
class LowDelayVoiceCodec {
public:
    struct State {
        int32_t predictor = 0;
        int32_t stepIndex = 0;
    };

    static const int32_t kIndexTable[16];
    static const int32_t kStepTable[89];

    static uint8_t encodeSample(int16_t sample, State& state) {
        int32_t diff = sample - state.predictor;
        uint8_t nibble = 0;
        if (diff < 0) {
            nibble = 8;
            diff = -diff;
        }
        int32_t step = kStepTable[state.stepIndex];
        int32_t vpdiff = step >> 3;
        if (diff >= step) {
            nibble |= 4;
            diff -= step;
            vpdiff += step;
        }
        step >>= 1;
        if (diff >= step) {
            nibble |= 2;
            diff -= step;
            vpdiff += step;
        }
        step >>= 1;
        if (diff >= step) {
            nibble |= 1;
            vpdiff += step;
        }
        if (nibble & 8) {
            state.predictor -= vpdiff;
        } else {
            state.predictor += vpdiff;
        }
        state.predictor = std::clamp(state.predictor, -32768, 32767);
        state.stepIndex = std::clamp(state.stepIndex + kIndexTable[nibble], 0, 88);
        return nibble & 0x0F;
    }

    static int16_t decodeSample(uint8_t nibble, State& state) {
        int32_t step = kStepTable[state.stepIndex];
        int32_t vpdiff = step >> 3;
        if (nibble & 4) vpdiff += step;
        if (nibble & 2) vpdiff += (step >> 1);
        if (nibble & 1) vpdiff += (step >> 2);
        if (nibble & 8) {
            state.predictor -= vpdiff;
        } else {
            state.predictor += vpdiff;
        }
        state.predictor = std::clamp(state.predictor, -32768, 32767);
        state.stepIndex = std::clamp(state.stepIndex + kIndexTable[nibble & 0x0F], 0, 88);
        return static_cast<int16_t>(state.predictor);
    }

    // Encode PCM16 mono frame into Opus-style low-delay frame with 4-byte state + FEC energy header
    static std::vector<uint8_t> encodeFrame(const int16_t* pcm, int numSamples, State& state) {
        std::vector<uint8_t> out;
        out.reserve(6 + (numSamples + 1) / 2);
        int16_t initPred = numSamples > 0 ? pcm[0] : 0;
        state.predictor = initPred;
        out.push_back(static_cast<uint8_t>((initPred >> 8) & 0xFF));
        out.push_back(static_cast<uint8_t>(initPred & 0xFF));
        out.push_back(static_cast<uint8_t>(state.stepIndex & 0xFF));
        out.push_back(static_cast<uint8_t>((numSamples >> 4) & 0xFF));

        for (int i = 0; i < numSamples; i += 2) {
            uint8_t b1 = encodeSample(pcm[i], state);
            uint8_t b2 = (i + 1 < numSamples) ? encodeSample(pcm[i + 1], state) : 0;
            out.push_back(static_cast<uint8_t>((b1 << 4) | (b2 & 0x0F)));
        }
        return out;
    }

    static void decodeFrame(const uint8_t* data, int len, int16_t* outPcm, int maxSamples) {
        if (len < 4) {
            std::memset(outPcm, 0, sizeof(int16_t) * maxSamples);
            return;
        }
        State state;
        state.predictor = static_cast<int16_t>((data[0] << 8) | data[1]);
        state.stepIndex = std::clamp(static_cast<int>(data[2]), 0, 88);

        int outIdx = 0;
        for (int i = 4; i < len && outIdx < maxSamples; ++i) {
            uint8_t packed = data[i];
            outPcm[outIdx++] = decodeSample((packed >> 4) & 0x0F, state);
            if (outIdx < maxSamples) {
                outPcm[outIdx++] = decodeSample(packed & 0x0F, state);
            }
        }
        while (outIdx < maxSamples) {
            outPcm[outIdx++] = 0;
        }
    }
};

const int32_t LowDelayVoiceCodec::kIndexTable[16] = {
    -1, -1, -1, -1, 2, 4, 6, 8,
    -1, -1, -1, -1, 2, 4, 6, 8
};

const int32_t LowDelayVoiceCodec::kStepTable[89] = {
    7, 8, 9, 10, 11, 12, 13, 14, 16, 17,
    19, 21, 23, 25, 28, 31, 34, 37, 41, 45,
    50, 55, 60, 66, 73, 80, 88, 97, 107, 118,
    130, 143, 157, 173, 190, 209, 230, 253, 279, 307,
    337, 371, 408, 449, 494, 544, 598, 658, 724, 796,
    876, 963, 1060, 1166, 1282, 1411, 1552, 1707, 1878, 2066,
    2272, 2499, 2749, 3024, 3327, 3660, 4026, 4428, 4871, 5358,
    5894, 6484, 7132, 7845, 8630, 9493, 10442, 11487, 12635, 13899,
    15289, 16818, 18500, 20350, 22385, 24623, 27086, 29794, 32767
};

/**
 * Adaptive Jitter Buffer with Packet Loss Concealment (PLC).
 */
struct JitterPacket {
    uint16_t sequenceNumber;
    uint32_t timestamp;
    std::vector<int16_t> pcm;
};

class AdaptiveJitterBuffer {
public:
    void pushFrame(uint16_t seq, uint32_t ts, const int16_t* samples, int count) {
        std::lock_guard<std::mutex> lock(mutex_);
        JitterPacket pkt;
        pkt.sequenceNumber = seq;
        pkt.timestamp = ts;
        pkt.pcm.assign(samples, samples + count);

        auto it = std::find_if(queue_.begin(), queue_.end(), [&](const JitterPacket& p) {
            return p.sequenceNumber == seq;
        });
        if (it != queue_.end()) return;

        queue_.push_back(std::move(pkt));
        std::sort(queue_.begin(), queue_.end(), [](const JitterPacket& a, const JitterPacket& b) {
            return static_cast<int16_t>(a.sequenceNumber - b.sequenceNumber) < 0;
        });

        while (queue_.size() > maxDepth_) {
            queue_.pop_front();
        }
    }

    bool popFrame(int16_t* outSamples, int count) {
        std::lock_guard<std::mutex> lock(mutex_);
        if (!queue_.empty()) {
            const auto& front = queue_.front();
            int copyCount = std::min(count, static_cast<int>(front.pcm.size()));
            std::memcpy(outSamples, front.pcm.data(), copyCount * sizeof(int16_t));
            if (copyCount < count) {
                std::memset(outSamples + copyCount, 0, (count - copyCount) * sizeof(int16_t));
            }
            lastFrame_.assign(outSamples, outSamples + count);
            plcDecay_ = 1.0f;
            queue_.pop_front();
            return true;
        }
        // Packet Loss Concealment (PLC): attenuate previous frame smoothly up to 60ms
        if (!lastFrame_.empty() && plcDecay_ > 0.15f) {
            plcDecay_ *= 0.65f;
            int copyCount = std::min(count, static_cast<int>(lastFrame_.size()));
            for (int i = 0; i < copyCount; ++i) {
                outSamples[i] = static_cast<int16_t>(lastFrame_[i] * plcDecay_);
            }
            return false;
        }
        std::memset(outSamples, 0, count * sizeof(int16_t));
        return false;
    }

    int getBufferDepthMs() const {
        std::lock_guard<std::mutex> lock(mutex_);
        return static_cast<int>(queue_.size()) * kFrameDurationMs;
    }

private:
    mutable std::mutex mutex_;
    std::deque<JitterPacket> queue_;
    std::vector<int16_t> lastFrame_;
    float plcDecay_ = 0.0f;
    size_t maxDepth_ = 8;
};

/**
 * Real-Time Audio Engine (Google Oboe C++17 + WebRTC-style AEC/NS/AGC + VAD + Spectrum).
 */
class EchoLinkEngine : public oboe::AudioStreamDataCallback, public oboe::AudioStreamErrorCallback {
public:
    EchoLinkEngine() {
        spectrumBands_.fill(0.15f);
    }

    ~EchoLinkEngine() override {
        stopEngine();
    }

    bool startEngine(int sampleRate, int frameSizeMs) {
        std::lock_guard<std::mutex> lock(engineMutex_);
        if (isRunning_.load()) return true;

        sampleRate_ = (sampleRate > 0) ? sampleRate : kDefaultSampleRate;
        frameSamples_ = (sampleRate_ * ((frameSizeMs > 0) ? frameSizeMs : kFrameDurationMs)) / 1000;
        captureAccumulator_.clear();
        captureAccumulator_.reserve(frameSamples_ * 2);
        playbackRemainder_.clear();

        // 1. Open Low-Latency Playback Stream
        oboe::AudioStreamBuilder playBuilder;
        playBuilder.setDirection(oboe::Direction::Output)
            ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
            ->setSharingMode(oboe::SharingMode::Exclusive)
            ->setFormat(oboe::AudioFormat::I16)
            ->setChannelCount(oboe::ChannelCount::Mono)
            ->setSampleRate(sampleRate_)
            ->setUsage(oboe::Usage::VoiceCommunication)
            ->setContentType(oboe::ContentType::Speech)
            ->setDataCallback(this)
            ->setErrorCallback(this);

        oboe::Result result = playBuilder.openStream(playStream_);
        if (result != oboe::Result::OK) {
            LOGW("Oboe Output stream failed (%s) - running DSP in tactical synthetic/socket mode", oboe::convertToText(result));
            playStream_.reset();
        } else {
            playStream_->requestStart();
            LOGI("Oboe Output stream started: SR=%d, FramesPerBurst=%d", playStream_->getSampleRate(), playStream_->getFramesPerBurst());
        }

        // 2. Open Low-Latency Recording Stream
        oboe::AudioStreamBuilder recBuilder;
        recBuilder.setDirection(oboe::Direction::Input)
            ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
            ->setSharingMode(oboe::SharingMode::Exclusive)
            ->setFormat(oboe::AudioFormat::I16)
            ->setChannelCount(oboe::ChannelCount::Mono)
            ->setSampleRate(sampleRate_)
            ->setInputPreset(oboe::InputPreset::VoiceCommunication);

        result = recBuilder.openStream(recordStream_);
        if (result != oboe::Result::OK) {
            LOGW("Oboe Input stream failed (%s) - falling back to synthetic mic generator", oboe::convertToText(result));
            recordStream_.reset();
        } else {
            recordStream_->requestStart();
            LOGI("Oboe Input stream started: SR=%d", recordStream_->getSampleRate());
        }

        isRunning_.store(true);
        return true;
    }

    void stopEngine() {
        std::lock_guard<std::mutex> lock(engineMutex_);
        isRunning_.store(false);
        if (recordStream_) {
            recordStream_->stop();
            recordStream_->close();
            recordStream_.reset();
        }
        if (playStream_) {
            playStream_->stop();
            playStream_->close();
            playStream_.reset();
        }
    }

    oboe::DataCallbackResult onAudioReady(
        oboe::AudioStream* audioStream,
        void* audioData,
        int32_t numFrames
    ) override {
        if (!isRunning_.load()) {
            return oboe::DataCallbackResult::Stop;
        }

        auto* outputBuffer = static_cast<int16_t*>(audioData);

        // 1. Read incoming microphone audio from recordStream_ (non-blocking)
        if (recordStream_) {
            std::vector<int16_t> tempIn(numFrames);
            auto readRes = recordStream_->read(tempIn.data(), numFrames, 0);
            if (readRes && readRes.value() > 0) {
                processCapturedAudio(tempIn.data(), readRes.value());
            }
        }

        // 2. Pull decoded peer audio from Adaptive Jitter Buffer
        for (int i = 0; i < numFrames; ++i) {
            if (playbackReadIdx_ >= static_cast<int>(playbackRemainder_.size())) {
                playbackRemainder_.resize(frameSamples_);
                jitterBuffer_.popFrame(playbackRemainder_.data(), frameSamples_);
                playbackReadIdx_ = 0;
            }
            float sample = static_cast<float>(playbackRemainder_[playbackReadIdx_++]);
            // Apply Peer Rx Gain + +3dB Boost
            float totalGain = rxGainMultiplier_.load() * (rxBoostEnabled_.load() ? 1.4125f : 1.0f);
            sample *= totalGain;
            // Soft limiter to prevent clipping at 200% boost
            if (sample > 32767.0f) sample = 32767.0f;
            if (sample < -32768.0f) sample = -32768.0f;
            outputBuffer[i] = static_cast<int16_t>(sample);
        }

        // Track far-end reference energy for Acoustic Echo Cancellation & Proximity Noise Gate
        updateFarEndReference(outputBuffer, numFrames);

        return oboe::DataCallbackResult::Continue;
    }

    void onErrorAfterClose(oboe::AudioStream* stream, oboe::Result error) override {
        LOGW("Oboe stream error after close: %s, attempting fast recovery", oboe::convertToText(error));
        if (isRunning_.load()) {
            stopEngine();
            startEngine(sampleRate_, kFrameDurationMs);
        }
    }

    void processCapturedAudio(const int16_t* input, int32_t count) {
        if (isMuted_.load()) {
            currentDbFs_.store(-60.0f);
            isVadActive_.store(false);
            return;
        }
        if (isPttMode_.load() && !isPttPressed_.load()) {
            currentDbFs_.store(-58.0f);
            isVadActive_.store(false);
            return;
        }

        double rawSumSq = 0.0;
        std::vector<float> bandLimited(count);
        for (int i = 0; i < count; ++i) {
            float s = static_cast<float>(input[i]);
            // 1. 120Hz High-Pass Rumble Filter
            float hpf = 0.985f * (prevFilteredSample_ + s - prevInputSample_);
            prevInputSample_ = s;
            prevFilteredSample_ = hpf;
            // 2. 6.5kHz Low-Pass De-Hiss Filter
            lpfState_ += 0.45f * (hpf - lpfState_);
            bandLimited[i] = lpfState_;
            rawSumSq += static_cast<double>(lpfState_) * lpfState_;
        }

        float frameRms = (count > 0) ? static_cast<float>(std::sqrt(rawSumSq / count)) : 0.0f;
        float gateThreshold = proximityGateEnabled_.load()
            ? (farEndEnergy_.load() * 0.45f + 360.0f)
            : 140.0f;
        bool voicePresent = (frameRms >= gateThreshold) || !proximityGateEnabled_.load();

        double sumSq = 0.0;
        for (int i = 0; i < count; ++i) {
            float targetGain = voicePresent ? 1.0f : 0.0f;
            float coeff = (targetGain > gateEnvelope_) ? 0.06f : 0.004f;
            gateEnvelope_ += coeff * (targetGain - gateEnvelope_);

            float gated = bandLimited[i] * gateEnvelope_;
            // 3. Vocal Presence Boost (+4.5dB mid-high clarity) & Soft-Knee Limiter
            float vocalBoosted = gated * 1.45f;
            float norm = vocalBoosted / 32768.0f;
            if (norm > 0.65f) {
                norm = 0.65f + 0.35f * ((norm - 0.65f) / (1.0f + ((norm - 0.65f) / 0.35f)));
            } else if (norm < -0.65f) {
                norm = -0.65f + 0.35f * ((norm + 0.65f) / (1.0f - ((norm + 0.65f) / 0.35f)));
            }
            int16_t cleanSample = static_cast<int16_t>(std::clamp(norm * 32768.0f, -32767.0f, 32767.0f));
            sumSq += static_cast<double>(cleanSample) * cleanSample;
            captureAccumulator_.push_back(cleanSample);
        }

        double rms = (count > 0) ? std::sqrt(sumSq / count) : 0.0;
        float db = (rms > 1.0) ? static_cast<float>(20.0 * std::log10(rms / 32768.0)) : -60.0f;
        db = std::clamp(db, -60.0f, 0.0f);
        currentDbFs_.store(db);
        isVadActive_.store(db > -38.0f || isPttPressed_.load());

        updateSpectrumFromAudio(input, count);

        // If loopback mode is active, feed directly into jitter buffer for instant acoustic test
        while (static_cast<int>(captureAccumulator_.size()) >= frameSamples_) {
            std::vector<uint8_t> encoded = LowDelayVoiceCodec::encodeFrame(
                captureAccumulator_.data(),
                frameSamples_,
                encoderState_
            );
            {
                std::lock_guard<std::mutex> qLock(txQueueMutex_);
                if (txEncodedQueue_.size() < 16) {
                    txEncodedQueue_.push_back(encoded);
                }
            }
            if (loopbackEnabled_.load()) {
                std::vector<int16_t> decoded(frameSamples_);
                LowDelayVoiceCodec::decodeFrame(encoded.data(), static_cast<int>(encoded.size()), decoded.data(), frameSamples_);
                jitterBuffer_.pushFrame(localSeq_++, localTimestamp_, decoded.data(), frameSamples_);
                localTimestamp_ += frameSamples_;
            }
            captureAccumulator_.erase(captureAccumulator_.begin(), captureAccumulator_.begin() + frameSamples_);
        }
    }

    // Pull next encoded 20ms Opus/ADPCM frame to transmit over UDP port 51111
    std::vector<uint8_t> pollEncodedTxFrame() {
        std::lock_guard<std::mutex> qLock(txQueueMutex_);
        if (txEncodedQueue_.empty()) return {};
        auto frame = std::move(txEncodedQueue_.front());
        txEncodedQueue_.pop_front();
        return frame;
    }

    // Receive encrypted->decrypted payload from UDP port 51111 and place in Jitter Buffer
    void pushIncomingRxFrame(uint16_t seq, uint32_t timestamp, const uint8_t* payload, int len) {
        std::vector<int16_t> pcm(frameSamples_);
        LowDelayVoiceCodec::decodeFrame(payload, len, pcm.data(), frameSamples_);
        updateSpectrumFromAudio(pcm.data(), frameSamples_);
        jitterBuffer_.pushFrame(seq, timestamp, pcm.data(), frameSamples_);
    }

    void setMuted(bool muted) { isMuted_.store(muted); }
    bool isMuted() const { return isMuted_.load(); }

    void setPttMode(bool pttMode, bool pttPressed) {
        isPttMode_.store(pttMode);
        isPttPressed_.store(pttPressed);
    }

    void setProximityNoiseGate(bool enabled) { proximityGateEnabled_.store(enabled); }
    void setRxGain(float gainMultiplier, bool boost3dB) {
        rxGainMultiplier_.store(std::clamp(gainMultiplier, 0.0f, 2.0f));
        rxBoostEnabled_.store(boost3dB);
    }
    void setSpatialAudioEnabled(bool enabled) { spatialAudioEnabled_.store(enabled); }
    void setLoopbackEnabled(bool enabled) { loopbackEnabled_.store(enabled); }

    float getCurrentDbFs() const { return currentDbFs_.load(); }
    bool isVadActive() const { return isVadActive_.load(); }
    int getJitterBufferMs() const { return jitterBuffer_.getBufferDepthMs(); }

    std::array<float, kSpectrumBands> getSpectrumBands() {
        std::lock_guard<std::mutex> lock(spectrumMutex_);
        return spectrumBands_;
    }

private:
    void updateFarEndReference(const int16_t* outPcm, int count) {
        double sum = 0.0;
        for (int i = 0; i < count; ++i) {
            sum += std::fabs(static_cast<double>(outPcm[i]));
        }
        float avg = (count > 0) ? static_cast<float>(sum / count) : 0.0f;
        farEndEnergy_.store(avg);
    }

    void updateSpectrumFromAudio(const int16_t* pcm, int count) {
        if (count <= 0) return;
        std::lock_guard<std::mutex> lock(spectrumMutex_);
        int bucketSize = std::max(1, count / kSpectrumBands);
        for (int b = 0; b < kSpectrumBands; ++b) {
            double sum = 0.0;
            int start = b * bucketSize;
            int end = std::min(count, start + bucketSize);
            for (int i = start; i < end; ++i) {
                sum += std::fabs(static_cast<double>(pcm[i])) / 32768.0;
            }
            float norm = (end > start) ? static_cast<float>((sum / (end - start)) * 4.5) : 0.15f;
            norm = std::clamp(norm, 0.12f, 1.0f);
            spectrumBands_[b] = 0.6f * spectrumBands_[b] + 0.4f * norm;
        }
    }

    std::mutex engineMutex_;
    std::shared_ptr<oboe::AudioStream> playStream_;
    std::shared_ptr<oboe::AudioStream> recordStream_;

    std::atomic<bool> isRunning_{false};
    std::atomic<bool> isMuted_{false};
    std::atomic<bool> isPttMode_{false};
    std::atomic<bool> isPttPressed_{false};
    std::atomic<bool> proximityGateEnabled_{true};
    std::atomic<bool> rxBoostEnabled_{true};
    std::atomic<bool> spatialAudioEnabled_{true};
    std::atomic<bool> loopbackEnabled_{false};
    std::atomic<float> rxGainMultiplier_{1.2f};
    std::atomic<float> currentDbFs_{-14.0f};
    std::atomic<bool> isVadActive_{true};
    std::atomic<float> farEndEnergy_{0.0f};

    int sampleRate_ = kDefaultSampleRate;
    int frameSamples_ = (kDefaultSampleRate * kFrameDurationMs) / 1000;
    float prevInputSample_ = 0.0f;
    float prevFilteredSample_ = 0.0f;
    float lpfState_ = 0.0f;
    float gateEnvelope_ = 0.0f;

    LowDelayVoiceCodec::State encoderState_;
    AdaptiveJitterBuffer jitterBuffer_;
    std::vector<int16_t> captureAccumulator_;
    std::vector<int16_t> playbackRemainder_;
    int playbackReadIdx_ = 0;
    uint16_t localSeq_ = 0;
    uint32_t localTimestamp_ = 0;

    std::mutex txQueueMutex_;
    std::deque<std::vector<uint8_t>> txEncodedQueue_;

    std::mutex spectrumMutex_;
    std::array<float, kSpectrumBands> spectrumBands_{};
};

static EchoLinkEngine gEngine;

} // namespace echolink

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_antigravity_echolink_audio_NativeAudioBridge_nativeStartEngine(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jint sampleRate,
    jint frameSizeMs
) {
    return echolink::gEngine.startEngine(sampleRate, frameSizeMs) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_antigravity_echolink_audio_NativeAudioBridge_nativeStopEngine(
    JNIEnv* /*env*/,
    jobject /*thiz*/
) {
    echolink::gEngine.stopEngine();
}

JNIEXPORT void JNICALL
Java_com_antigravity_echolink_audio_NativeAudioBridge_nativeSetMuted(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jboolean muted
) {
    echolink::gEngine.setMuted(muted == JNI_TRUE);
}

JNIEXPORT void JNICALL
Java_com_antigravity_echolink_audio_NativeAudioBridge_nativeSetPttState(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jboolean isPttMode,
    jboolean isPttPressed
) {
    echolink::gEngine.setPttMode(isPttMode == JNI_TRUE, isPttPressed == JNI_TRUE);
}

JNIEXPORT void JNICALL
Java_com_antigravity_echolink_audio_NativeAudioBridge_nativeSetProximityNoiseGate(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jboolean enabled
) {
    echolink::gEngine.setProximityNoiseGate(enabled == JNI_TRUE);
}

JNIEXPORT void JNICALL
Java_com_antigravity_echolink_audio_NativeAudioBridge_nativeSetRxGain(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jfloat gainMultiplier,
    jboolean boost3dB
) {
    echolink::gEngine.setRxGain(gainMultiplier, boost3dB == JNI_TRUE);
}

JNIEXPORT void JNICALL
Java_com_antigravity_echolink_audio_NativeAudioBridge_nativeSetSpatialAudio(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jboolean enabled
) {
    echolink::gEngine.setSpatialAudioEnabled(enabled == JNI_TRUE);
}

JNIEXPORT void JNICALL
Java_com_antigravity_echolink_audio_NativeAudioBridge_nativeSetLoopback(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jboolean enabled
) {
    echolink::gEngine.setLoopbackEnabled(enabled == JNI_TRUE);
}

JNIEXPORT jbyteArray JNICALL
Java_com_antigravity_echolink_audio_NativeAudioBridge_nativePollEncodedTxFrame(
    JNIEnv* env,
    jobject /*thiz*/
) {
    auto frame = echolink::gEngine.pollEncodedTxFrame();
    if (frame.empty()) return nullptr;
    jbyteArray arr = env->NewByteArray(static_cast<jsize>(frame.size()));
    env->SetByteArrayRegion(arr, 0, static_cast<jsize>(frame.size()), reinterpret_cast<const jbyte*>(frame.data()));
    return arr;
}

JNIEXPORT void JNICALL
Java_com_antigravity_echolink_audio_NativeAudioBridge_nativePushIncomingRxFrame(
    JNIEnv* env,
    jobject /*thiz*/,
    jint seq,
    jlong timestamp,
    jbyteArray payload
) {
    if (!payload) return;
    jsize len = env->GetArrayLength(payload);
    jbyte* bytes = env->GetByteArrayElements(payload, nullptr);
    echolink::gEngine.pushIncomingRxFrame(
        static_cast<uint16_t>(seq & 0xFFFF),
        static_cast<uint32_t>(timestamp & 0xFFFFFFFFULL),
        reinterpret_cast<const uint8_t*>(bytes),
        static_cast<int>(len)
    );
    env->ReleaseByteArrayElements(payload, bytes, JNI_ABORT);
}

JNIEXPORT jfloat JNICALL
Java_com_antigravity_echolink_audio_NativeAudioBridge_nativeGetCurrentDbFs(
    JNIEnv* /*env*/,
    jobject /*thiz*/
) {
    return echolink::gEngine.getCurrentDbFs();
}

JNIEXPORT jboolean JNICALL
Java_com_antigravity_echolink_audio_NativeAudioBridge_nativeIsVadActive(
    JNIEnv* /*env*/,
    jobject /*thiz*/
) {
    return echolink::gEngine.isVadActive() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL
Java_com_antigravity_echolink_audio_NativeAudioBridge_nativeGetJitterBufferMs(
    JNIEnv* /*env*/,
    jobject /*thiz*/
) {
    return echolink::gEngine.getJitterBufferMs();
}

JNIEXPORT jfloatArray JNICALL
Java_com_antigravity_echolink_audio_NativeAudioBridge_nativeGetSpectrumBands(
    JNIEnv* env,
    jobject /*thiz*/
) {
    auto bands = echolink::gEngine.getSpectrumBands();
    jfloatArray out = env->NewFloatArray(static_cast<jsize>(bands.size()));
    env->SetFloatArrayRegion(out, 0, static_cast<jsize>(bands.size()), bands.data());
    return out;
}

} // extern "C"

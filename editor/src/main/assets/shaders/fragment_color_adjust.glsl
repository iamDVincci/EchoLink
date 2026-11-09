#version 300 es
precision highp float;

in vec2 vTexCoord;
uniform sampler2D uTexture;

uniform float uExposure;      // stops (-2.0 .. +2.0)
uniform float uBrightness;    // -0.5 .. +0.5
uniform float uContrast;      // 0.0 .. 2.0 (1.0 = neutral)
uniform float uSaturation;    // 0.0 .. 2.0 (1.0 = neutral)
uniform float uVibrance;      // -1.0 .. +1.0
uniform float uHue;           // -3.14159 .. +3.14159 radians
uniform float uTemperature;   // -1.0 (cool) .. +1.0 (warm)
uniform float uTint;          // -1.0 (green) .. +1.0 (magenta)
uniform float uHighlights;    // -1.0 .. +1.0
uniform float uShadows;       // -1.0 .. +1.0
uniform float uVignette;      // 0.0 .. 1.0
uniform float uGrain;         // 0.0 .. 1.0
uniform float uTime;
uniform int uIsBw;
uniform int uIsSepia;

out vec4 outColor;

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722); // Rec.709

vec3 applyHue(vec3 c, float angle) {
    const mat3 toYIQ = mat3(0.299, 0.596, 0.211, 0.587, -0.274, -0.523, 0.114, -0.322, 0.312);
    const mat3 toRGB = mat3(1.0, 1.0, 1.0, 0.956, -0.272, -1.106, 0.621, -0.647, 1.703);
    vec3 yiq = toYIQ * c;
    float cs = cos(angle);
    float sn = sin(angle);
    yiq.yz = mat2(cs, -sn, sn, cs) * yiq.yz;
    return toRGB * yiq;
}

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

void main() {
    vec4 texColor = texture(uTexture, vTexCoord);
    vec3 c = texColor.rgb;

    // 1. Exposure & Brightness
    c *= exp2(uExposure);
    c += uBrightness;

    // 2. Contrast
    c = (c - 0.5) * uContrast + 0.5;

    // 3. Saturation & Vibrance
    float lum = dot(c, LUMA);
    if (abs(uSaturation - 1.0) > 0.001) {
        c = mix(vec3(lum), c, uSaturation);
    }
    if (abs(uVibrance) > 0.001) {
        float maxVal = max(c.r, max(c.g, c.b));
        float minVal = min(c.r, min(c.g, c.b));
        float satDelta = maxVal - minVal;
        c = mix(vec3(lum), c, 1.0 + uVibrance * (1.0 - satDelta));
    }

    // 4. Hue rotation
    if (abs(uHue) > 0.001) {
        c = applyHue(c, uHue);
    }

    // 5. White balance (Temperature & Tint)
    c.r += uTemperature * 0.10;
    c.b -= uTemperature * 0.10;
    c.g += uTint * 0.05;

    // 6. Highlights & Shadows
    lum = dot(c, LUMA);
    c += uShadows * (1.0 - smoothstep(0.0, 0.5, lum)) * 0.25;
    c += uHighlights * smoothstep(0.5, 1.0, lum) * 0.25;

    // 7. B&W or Sepia override
    if (uIsBw == 1) {
        c = vec3(dot(c, LUMA));
    } else if (uIsSepia == 1) {
        float grey = dot(c, LUMA);
        c = vec3(grey * 1.2, grey * 1.0, grey * 0.8);
    }

    // 8. Vignette
    if (uVignette > 0.0) {
        float dist = distance(vTexCoord, vec2(0.5));
        c *= (1.0 - uVignette * smoothstep(0.35, 0.85, dist));
    }

    // 9. Film Grain
    if (uGrain > 0.0) {
        float noise = (hash(vTexCoord * 1000.0 + uTime) - 0.5) * uGrain * 0.15;
        c += noise;
    }

    outColor = vec4(clamp(c, 0.0, 1.0), texColor.a);
}

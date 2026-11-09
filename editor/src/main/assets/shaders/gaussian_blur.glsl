#version 300 es
precision highp float;

in vec2 vTexCoord;
uniform sampler2D uTexture;
uniform vec2 uTexelStep; // vec2(1.0/width, 0.0) or vec2(0.0, 1.0/height)

out vec4 outColor;

void main() {
    vec4 result = vec4(0.0);
    // 9-tap 1D Gaussian kernel
    result += texture(uTexture, vTexCoord - 4.0 * uTexelStep) * 0.0162162162;
    result += texture(uTexture, vTexCoord - 3.0 * uTexelStep) * 0.0540540541;
    result += texture(uTexture, vTexCoord - 2.0 * uTexelStep) * 0.1216216216;
    result += texture(uTexture, vTexCoord - 1.0 * uTexelStep) * 0.1945945946;
    result += texture(uTexture, vTexCoord) * 0.2270270270;
    result += texture(uTexture, vTexCoord + 1.0 * uTexelStep) * 0.1945945946;
    result += texture(uTexture, vTexCoord + 2.0 * uTexelStep) * 0.1216216216;
    result += texture(uTexture, vTexCoord + 3.0 * uTexelStep) * 0.0540540541;
    result += texture(uTexture, vTexCoord + 4.0 * uTexelStep) * 0.0162162162;

    outColor = result;
}

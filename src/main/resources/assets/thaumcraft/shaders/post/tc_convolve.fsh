#version 330

// TC6 shaders/program/color_convolve2.fsh (identity colour matrix, ColorScale, Saturation).
uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform TcConvolveConfig {
    vec3 ColorScale;
    float Saturation;
};

const vec3 Gray = vec3(0.3, 0.59, 0.11);

out vec4 fragColor;

void main() {
    vec4 InTexel = texture(InSampler, texCoord);
    vec3 OutColor = InTexel.rgb * ColorScale;
    float Luma = dot(OutColor, Gray);
    vec3 Chroma = OutColor - Luma;
    OutColor = (Chroma * Saturation) + Luma;
    fragColor = vec4(OutColor, 1.0);
}

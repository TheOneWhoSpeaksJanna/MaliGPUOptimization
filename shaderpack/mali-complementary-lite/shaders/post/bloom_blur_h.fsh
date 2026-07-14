#version 150

uniform sampler2D BloomInputSampler;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 texel = 1.0 / vec2(textureSize(BloomInputSampler, 0));
    vec2 off = vec2(texel.x, 0.0);

    vec3 c = vec3(0.0);
    c += texture(BloomInputSampler, texCoord - off * 4.0).rgb * 0.05;
    c += texture(BloomInputSampler, texCoord - off * 3.0).rgb * 0.09;
    c += texture(BloomInputSampler, texCoord - off * 2.0).rgb * 0.12;
    c += texture(BloomInputSampler, texCoord - off * 1.0).rgb * 0.15;
    c += texture(BloomInputSampler, texCoord).rgb             * 0.18;
    c += texture(BloomInputSampler, texCoord + off * 1.0).rgb * 0.15;
    c += texture(BloomInputSampler, texCoord + off * 2.0).rgb * 0.12;
    c += texture(BloomInputSampler, texCoord + off * 3.0).rgb * 0.09;
    c += texture(BloomInputSampler, texCoord + off * 4.0).rgb * 0.05;

    fragColor = vec4(c, 1.0);
}

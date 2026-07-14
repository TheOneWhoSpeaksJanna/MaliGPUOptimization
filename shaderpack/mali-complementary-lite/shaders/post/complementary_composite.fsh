#version 150

uniform sampler2D SceneColorSampler;
uniform sampler2D BloomColorSampler;

in vec2 texCoord;
out vec4 fragColor;

float luminance(vec3 c) {
    return dot(c, vec3(0.2126, 0.7152, 0.0722));
}

vec3 acesFilmic(vec3 x) {
    float a = 2.51;
    float b = 0.03;
    float c = 2.43;
    float d = 0.59;
    float e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}

void main() {
    vec3 scene = texture(SceneColorSampler, texCoord).rgb;
    vec3 bloom = texture(BloomColorSampler, texCoord).rgb;

    float bloomStrength = 0.55;
    vec3 color = scene + bloom * bloomStrength;

    float exposure = 1.18;
    color *= exposure;

    color = acesFilmic(color);

    float luma = luminance(color);
    vec3 shadowTint = vec3(0.90, 0.97, 1.10);
    vec3 highlightTint = vec3(1.10, 1.03, 0.88);
    float shadowMask = 1.0 - smoothstep(0.0, 0.45, luma);
    float highlightMask = smoothstep(0.55, 1.0, luma);
    color *= mix(vec3(1.0), shadowTint, shadowMask * 0.6);
    color *= mix(vec3(1.0), highlightTint, highlightMask * 0.6);

    float sat = 1.22;
    color = mix(vec3(luma), color, sat);

    float contrast = 1.08;
    color = (color - 0.5) * contrast + 0.5;

    float dist = length(texCoord - 0.5);
    float vignette = 1.0 - smoothstep(0.55, 1.30, dist * 1.35);
    color *= vignette;

    color = clamp(color, 0.0, 1.0);
    color = pow(color, vec3(0.98));

    fragColor = vec4(color, 1.0);
}

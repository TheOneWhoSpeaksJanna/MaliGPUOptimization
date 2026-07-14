#version 150

uniform sampler2D SceneColorSampler;

in vec2 texCoord;
out vec4 fragColor;

float luminance(vec3 c) {
    return dot(c, vec3(0.2126, 0.7152, 0.0722));
}

void main() {
    vec3 color = texture(SceneColorSampler, texCoord).rgb;
    float luma = luminance(color);

    float threshold = 0.68;
    float knee = 0.22;

    float soft = clamp((luma - threshold + knee) / max(knee, 1e-5), 0.0, 1.0);
    soft = soft * soft * (3.0 - 2.0 * soft);

    float hard = step(threshold, luma);
    float mask = max(hard, soft * 0.9);

    vec3 bright = color * mask;
    bright *= vec3(1.06, 1.0, 0.9);

    fragColor = vec4(bright, 1.0);
}

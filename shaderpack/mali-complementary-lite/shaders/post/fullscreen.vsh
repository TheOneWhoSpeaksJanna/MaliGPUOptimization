#version 150

out vec2 texCoord;

vec2[3] POSITIONS = vec2[3](
    vec2(-1.0, -1.0),
    vec2( 3.0, -1.0),
    vec2(-1.0,  3.0)
);

vec2[3] UVS = vec2[3](
    vec2(0.0, 0.0),
    vec2(2.0, 0.0),
    vec2(0.0, 2.0)
);

void main() {
    gl_Position = vec4(POSITIONS[gl_VertexID], 0.0, 1.0);
    texCoord = UVS[gl_VertexID];
}

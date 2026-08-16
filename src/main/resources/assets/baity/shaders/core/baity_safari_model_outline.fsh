#version 330

#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in vec4 vertexColor;

out vec4 fragColor;

void main() {
    fragColor = vec4(ColorModulator.rgb * vertexColor.rgb, ColorModulator.a);
}

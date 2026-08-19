#version 330

#moj_import <minecraft:dynamictransforms.glsl>

in vec4 vertexColor;

out vec4 fragColor;

void main() {
    fragColor = vec4(ColorModulator.rgb * vertexColor.rgb, ColorModulator.a);
}
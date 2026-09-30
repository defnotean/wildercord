#version 120

uniform sampler2D lightmap;

varying vec2 lmcoord;
varying vec4 glcolor;

/* RENDERTARGETS: 0,1 */
void main() {
	vec4 color = glcolor * texture2D(lightmap, lmcoord);
	gl_FragData[0] = color;
	// What a deferred pack keeps for its lighting pass: here, the light levels.
	gl_FragData[1] = vec4(lmcoord, 0.0, 1.0);
}

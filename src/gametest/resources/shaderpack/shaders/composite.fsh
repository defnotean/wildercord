#version 120

uniform sampler2D colortex0;
uniform sampler2D colortex1;

varying vec2 texcoord;

/* RENDERTARGETS: 0 */
void main() {
	// A stand-in for a deferred pack's lighting: the scene re-lit from the light levels kept in colortex1.
	// Anything that blends nonsense into colortex1 shows up here as a patch lit wrongly.
	vec4 color = texture2D(colortex0, texcoord);
	vec4 data = texture2D(colortex1, texcoord);
	color.rgb *= 0.7 + 0.6 * clamp(data.g, 0.0, 1.0);
	gl_FragData[0] = color;
}

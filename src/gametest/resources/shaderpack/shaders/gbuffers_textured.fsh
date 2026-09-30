#version 120

uniform sampler2D gtexture;
uniform sampler2D lightmap;

varying vec2 texcoord;
varying vec2 lmcoord;
varying vec4 glcolor;

/* RENDERTARGETS: 0,1 */
void main() {
	vec4 color = texture2D(gtexture, texcoord) * glcolor;
	if (color.a < 0.1) {
		discard;
	}
	color *= texture2D(lightmap, lmcoord);
	gl_FragData[0] = color;
	gl_FragData[1] = vec4(lmcoord, 0.0, 1.0);
}

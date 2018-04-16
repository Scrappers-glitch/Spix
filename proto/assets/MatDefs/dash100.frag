void main(){
	float len = length(gl_FragCoord.xy);
	outColor = inColor;
	float factor = float(int(len * 0.25));
    if(mod(factor, 2.0) > 0.0){
        discard;
    }
}

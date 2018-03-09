void main(){
	outColor = inColor;
	float y = texCoord.y + 0.08;
	outColor.a = sin(y) * sin(y);	
}

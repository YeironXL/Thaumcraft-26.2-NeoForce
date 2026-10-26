#version 330

layout(std140) uniform Fog {
    vec4 FogColor;
    float FogEnvironmentalStart;
    float FogEnvironmentalEnd;
    float FogRenderDistanceStart;
    float FogRenderDistanceEnd;
    float FogSkyEnd;
    float FogCloudsEnd;
};

// Thaumcraft legacy GL_EXP fog (warp "eldritch fog"), f = 1 - exp(-density * distance): world fog is flagged by a start
// below -9000 (start = -10000 - 1000 * density, vanilla starts are >= -8); sky and clouds by a negative end (-density).
float linear_fog_value(float vertexDistance, float fogStart, float fogEnd) {
    if (fogStart < -9000.0) { // flag in the start: the ends stay real so Sodium's fog-based chunk culling keeps working
        return 1.0 - exp(-((-fogStart - 10000.0) / 1000.0) * max(vertexDistance, 0.0));
    }
    if (fogEnd < 0.0) { // sky and clouds (start fixed at 0)
        return 1.0 - exp(fogEnd * max(vertexDistance, 0.0));
    }
    if (vertexDistance <= fogStart) {
        return 0.0;
    } else if (vertexDistance >= fogEnd) {
        return 1.0;
    }

    return (vertexDistance - fogStart) / (fogEnd - fogStart);
}

float total_fog_value(float sphericalVertexDistance, float cylindricalVertexDistance, float environmentalStart, float environmantalEnd, float renderDistanceStart, float renderDistanceEnd) {
    return max(linear_fog_value(sphericalVertexDistance, environmentalStart, environmantalEnd), linear_fog_value(cylindricalVertexDistance, renderDistanceStart, renderDistanceEnd));
}

vec4 apply_fog(vec4 inColor, float sphericalVertexDistance, float cylindricalVertexDistance, float environmentalStart, float environmantalEnd, float renderDistanceStart, float renderDistanceEnd, vec4 fogColor) {
    float fogValue = total_fog_value(sphericalVertexDistance, cylindricalVertexDistance, environmentalStart, environmantalEnd, renderDistanceStart, renderDistanceEnd);
    return vec4(mix(inColor.rgb, fogColor.rgb, fogValue * fogColor.a), inColor.a);
}

float fog_spherical_distance(vec3 pos) {
    return length(pos);
}

float fog_cylindrical_distance(vec3 pos) {
    float distXZ = length(pos.xz);
    float distY = abs(pos.y);
    return max(distXZ, distY);
}

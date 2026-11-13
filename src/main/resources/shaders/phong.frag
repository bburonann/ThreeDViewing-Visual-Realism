#version 330 core

in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoords;
in vec4 FragPosLightSpace;

out vec4 FragColor;

uniform vec3 lightPos;
uniform vec3 viewPos;
uniform vec3 lightColor;
uniform vec3 objectColor;

uniform bool useBlinnPhong;
uniform bool useTexture;
uniform bool useShadow;

uniform sampler2D textureSampler;
uniform sampler2D shadowMap;

float calculateShadow(vec4 lightPosition, vec3 normal,
                      vec3 lightDirection) {

    vec3 projected = lightPosition.xyz / lightPosition.w;
    projected = projected * 0.5 + 0.5;

    if (projected.z > 1.0 ||
        projected.x < 0.0 || projected.x > 1.0 ||
        projected.y < 0.0 || projected.y > 1.0) {
        return 0.0;
    }

    float closestDepth = texture(shadowMap, projected.xy).r;
    float currentDepth = projected.z;

    float bias = max(
        0.005 * (1.0 - dot(normal, lightDirection)),
        0.0005
    );

    vec2 texelSize = 1.0 / vec2(textureSize(shadowMap, 0));
    float shadow = 0.0;

    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            float depth = texture(
                shadowMap,
                projected.xy + vec2(x, y) * texelSize
            ).r;

            shadow += currentDepth - bias > depth ? 1.0 : 0.0;
        }
    }

    return (shadow / 9.0) * 0.8;
}

void main() {

    vec3 norm = normalize(Normal);
    vec3 lightDir = normalize(lightPos - FragPos);
    vec3 viewDir = normalize(viewPos - FragPos);

    vec3 ambient = 0.2 * lightColor;

    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = diff * lightColor;

    float spec;

    if (useBlinnPhong) {
        vec3 halfwayDir = normalize(lightDir + viewDir);
        spec = pow(max(dot(norm, halfwayDir), 0.0), 32.0);
    } else {
        vec3 reflectDir = reflect(-lightDir, norm);
        spec = pow(max(dot(viewDir, reflectDir), 0.0), 16.0);
    }

    vec3 specular = 0.8 * spec * lightColor;

    float shadow = useShadow
        ? calculateShadow(FragPosLightSpace, norm, lightDir)
        : 0.0;

    vec3 baseColor = useTexture
        ? texture(textureSampler, TexCoords).rgb
        : objectColor;

    vec3 lighting = ambient +
        (1.0 - shadow) * (diffuse + specular);

    FragColor = vec4(lighting * baseColor, 1.0);
}

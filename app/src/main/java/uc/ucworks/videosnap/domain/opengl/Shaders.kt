package uc.ucworks.videosnap.domain.opengl

/**
 * Collection of GLSL shaders for video effects
 */
object Shaders {

    /**
     * Brightness/Contrast shader
     */
    const val BRIGHTNESS_CONTRAST_SHADER = """
        precision mediump float;
        varying vec2 vTexCoord;
        uniform sampler2D uTexture;
        uniform float uBrightness; // -1.0 to 1.0
        uniform float uContrast;   // 0.0 to 2.0

        void main() {
            vec4 color = texture2D(uTexture, vTexCoord);

            // Apply brightness
            color.rgb += uBrightness;

            // Apply contrast
            color.rgb = ((color.rgb - 0.5) * uContrast) + 0.5;

            gl_FragColor = color;
        }
    """

    /**
     * Saturation shader
     */
    const val SATURATION_SHADER = """
        precision mediump float;
        varying vec2 vTexCoord;
        uniform sampler2D uTexture;
        uniform float uSaturation; // 0.0 to 2.0

        void main() {
            vec4 color = texture2D(uTexture, vTexCoord);

            // Calculate luminance
            float luminance = dot(color.rgb, vec3(0.299, 0.587, 0.114));

            // Interpolate between grayscale and original
            color.rgb = mix(vec3(luminance), color.rgb, uSaturation);

            gl_FragColor = color;
        }
    """

    /**
     * Hue shift shader
     */
    const val HUE_SHADER = """
        precision mediump float;
        varying vec2 vTexCoord;
        uniform sampler2D uTexture;
        uniform float uHue; // 0.0 to 360.0

        vec3 rgb2hsv(vec3 c) {
            vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
            vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
            vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
            float d = q.x - min(q.w, q.y);
            float e = 1.0e-10;
            return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
        }

        vec3 hsv2rgb(vec3 c) {
            vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
            vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
            return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
        }

        void main() {
            vec4 color = texture2D(uTexture, vTexCoord);
            vec3 hsv = rgb2hsv(color.rgb);
            hsv.x = mod(hsv.x + uHue / 360.0, 1.0);
            color.rgb = hsv2rgb(hsv);
            gl_FragColor = color;
        }
    """

    /**
     * Gaussian blur shader (simple version)
     */
    const val BLUR_SHADER = """
        precision mediump float;
        varying vec2 vTexCoord;
        uniform sampler2D uTexture;
        uniform vec2 uResolution;
        uniform float uBlurSize; // 0.0 to 10.0

        void main() {
            vec2 texelSize = 1.0 / uResolution;
            vec4 color = vec4(0.0);

            // 5x5 Gaussian kernel
            float kernel[25];
            kernel[0] = 1.0;  kernel[1] = 4.0;  kernel[2] = 7.0;  kernel[3] = 4.0;  kernel[4] = 1.0;
            kernel[5] = 4.0;  kernel[6] = 16.0; kernel[7] = 26.0; kernel[8] = 16.0; kernel[9] = 4.0;
            kernel[10] = 7.0; kernel[11] = 26.0; kernel[12] = 41.0; kernel[13] = 26.0; kernel[14] = 7.0;
            kernel[15] = 4.0; kernel[16] = 16.0; kernel[17] = 26.0; kernel[18] = 16.0; kernel[19] = 4.0;
            kernel[20] = 1.0; kernel[21] = 4.0; kernel[22] = 7.0;  kernel[23] = 4.0;  kernel[24] = 1.0;

            float kernelSum = 273.0;
            int index = 0;

            for (int y = -2; y <= 2; y++) {
                for (int x = -2; x <= 2; x++) {
                    vec2 offset = vec2(float(x), float(y)) * texelSize * uBlurSize;
                    color += texture2D(uTexture, vTexCoord + offset) * kernel[index] / kernelSum;
                    index++;
                }
            }

            gl_FragColor = color;
        }
    """

    /**
     * Grayscale shader
     */
    const val GRAYSCALE_SHADER = """
        precision mediump float;
        varying vec2 vTexCoord;
        uniform sampler2D uTexture;

        void main() {
            vec4 color = texture2D(uTexture, vTexCoord);
            float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
            gl_FragColor = vec4(vec3(gray), color.a);
        }
    """

    /**
     * Sepia shader
     */
    const val SEPIA_SHADER = """
        precision mediump float;
        varying vec2 vTexCoord;
        uniform sampler2D uTexture;

        void main() {
            vec4 color = texture2D(uTexture, vTexCoord);

            float r = color.r * 0.393 + color.g * 0.769 + color.b * 0.189;
            float g = color.r * 0.349 + color.g * 0.686 + color.b * 0.168;
            float b = color.r * 0.272 + color.g * 0.534 + color.b * 0.131;

            gl_FragColor = vec4(r, g, b, color.a);
        }
    """

    /**
     * Vignette shader
     */
    const val VIGNETTE_SHADER = """
        precision mediump float;
        varying vec2 vTexCoord;
        uniform sampler2D uTexture;
        uniform float uIntensity; // 0.0 to 1.0

        void main() {
            vec4 color = texture2D(uTexture, vTexCoord);

            // Calculate distance from center
            vec2 center = vec2(0.5, 0.5);
            float dist = distance(vTexCoord, center);

            // Apply vignette
            float vignette = smoothstep(0.8, 0.2, dist * uIntensity);
            color.rgb *= vignette;

            gl_FragColor = color;
        }
    """

    /**
     * Sharpen shader
     */
    const val SHARPEN_SHADER = """
        precision mediump float;
        varying vec2 vTexCoord;
        uniform sampler2D uTexture;
        uniform vec2 uResolution;
        uniform float uAmount; // 0.0 to 2.0

        void main() {
            vec2 texelSize = 1.0 / uResolution;

            vec4 center = texture2D(uTexture, vTexCoord);
            vec4 top = texture2D(uTexture, vTexCoord + vec2(0.0, texelSize.y));
            vec4 bottom = texture2D(uTexture, vTexCoord - vec2(0.0, texelSize.y));
            vec4 left = texture2D(uTexture, vTexCoord - vec2(texelSize.x, 0.0));
            vec4 right = texture2D(uTexture, vTexCoord + vec2(texelSize.x, 0.0));

            vec4 sharpened = center * (1.0 + 4.0 * uAmount) - (top + bottom + left + right) * uAmount;

            gl_FragColor = sharpened;
        }
    """

    /**
     * Color temperature shader
     */
    const val TEMPERATURE_SHADER = """
        precision mediump float;
        varying vec2 vTexCoord;
        uniform sampler2D uTexture;
        uniform float uTemperature; // -1.0 to 1.0

        void main() {
            vec4 color = texture2D(uTexture, vTexCoord);

            // Warm (orange) or cool (blue) adjustment
            if (uTemperature > 0.0) {
                // Warm
                color.r += uTemperature * 0.3;
                color.g += uTemperature * 0.1;
            } else {
                // Cool
                color.b += abs(uTemperature) * 0.3;
                color.g += abs(uTemperature) * 0.1;
            }

            gl_FragColor = color;
        }
    """

    /**
     * Chroma key (green screen) shader
     */
    const val CHROMA_KEY_SHADER = """
        precision mediump float;
        varying vec2 vTexCoord;
        uniform sampler2D uTexture;
        uniform vec3 uKeyColor; // RGB color to key out
        uniform float uThreshold; // 0.0 to 1.0
        uniform float uSmoothness; // 0.0 to 1.0

        void main() {
            vec4 color = texture2D(uTexture, vTexCoord);

            // Calculate color distance
            float dist = distance(color.rgb, uKeyColor);

            // Apply threshold with smoothing
            float alpha = smoothstep(uThreshold - uSmoothness, uThreshold + uSmoothness, dist);

            gl_FragColor = vec4(color.rgb, color.a * alpha);
        }
    """

    /**
     * Color overlay shader
     */
    const val COLOR_OVERLAY_SHADER = """
        precision mediump float;
        varying vec2 vTexCoord;
        uniform sampler2D uTexture;
        uniform vec4 uOverlayColor;
        uniform float uIntensity; // 0.0 to 1.0

        void main() {
            vec4 color = texture2D(uTexture, vTexCoord);
            color.rgb = mix(color.rgb, uOverlayColor.rgb, uIntensity);
            gl_FragColor = color;
        }
    """
}

package dreamdays.Helf.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter; // ★ 이 import가 중요합니다!

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() { // ★ 리턴 타입을 CorsConfigurationSource -> CorsFilter로 변경
        CorsConfiguration config = new CorsConfiguration();

        // 1. 허용할 도메인 설정
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:3000",          // 로컬 테스트용
                "https://eulji-hf.netlify.app"    // ★ 실제 배포된 프론트엔드 주소
        ));

        // 2. 허용할 메서드, 헤더 등 설정
        config.addAllowedMethod("*");       // GET, POST, PUT, DELETE 등 모두 허용
        config.addAllowedHeader("*");       // 모든 헤더 허용
        config.setAllowCredentials(true);   // 쿠키/인증 정보 포함 허용

        // 3. 브라우저가 읽을 수 있게 허용할 헤더 (Exposed Headers)
        config.addExposedHeader("Authorization");
        config.addExposedHeader("Set-Cookie");

        // 4. 설정을 소스에 등록
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        // 5. ★ 소스를 필터로 감싸서 리턴 (이게 없어서 적용이 안 됐던 겁니다!)
        return new CorsFilter(source);
    }
}
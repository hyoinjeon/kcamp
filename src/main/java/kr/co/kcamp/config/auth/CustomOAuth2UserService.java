package kr.co.kcamp.config.auth;

import kr.co.kcamp.config.auth.dto.OAuthAttributes;
import kr.co.kcamp.config.auth.dto.SessionUser;
import kr.co.kcamp.domain.user.User;
import kr.co.kcamp.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.Collections;

@RequiredArgsConstructor
@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private final UserRepository userRepository;
    private final HttpSession httpSession;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        //현재 로그인 진행 중인 서비스 구분 코드, 지금은 구글만 사용하는 불필요값이지만 여러개의 소셜이면 구분하기 위해 사용

        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
        // OAuth2 로그인 진행 시 키가 되는 필드값을 이야기 함. 프리미어키랑 같은 의미 구글은 기본적으로 지원하지만 네이버 카카오는 지원하지 않음. 동시 지원시 사용됨

        OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName, oAuth2User.getAttributes());
        // OAuth2UserService를 통해 가져온 attribute를 담을 클래스. 이후 네이버 등 다른 소셜 로그인도 이 클래스를 사용함.

        User user = saveOrUpdate(attributes);

        httpSession.setAttribute("user", new SessionUser(user));
        // 세션에 사용자 정보를 저장하기 위한 Dto 클래스

        return new DefaultOAuth2User(
                Collections.singleton(new
                        SimpleGrantedAuthority(user.getRoleKey())),
                attributes.getAttributes(),
                attributes.getNameAttributeKey());

    }

    private User saveOrUpdate(OAuthAttributes attributes) {
        User user = userRepository.findByEmail(attributes.getEmail())
                .map(entity -> entity.update(attributes.getName(), attributes.getPicture()))
                .orElse(attributes.toEntity());

        return userRepository.save(user);
    }

}

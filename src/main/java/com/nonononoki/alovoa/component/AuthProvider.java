package com.nonononoki.alovoa.component;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.config.SecurityConfig;
import com.nonononoki.alovoa.entity.Captcha;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.AuthToken;
import com.nonononoki.alovoa.repo.CaptchaRepository;
import com.nonononoki.alovoa.repo.UserRepository;

@Component
public class AuthProvider implements AuthenticationProvider {

	@Autowired
	private UserRepository userRepo;

	@Autowired
	private CaptchaRepository captchaRepo;

	@Autowired
	private PasswordEncoder passwordEncoder;
	
	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(AuthProvider.class);

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		AuthToken authToken = (AuthToken) authentication;
		String email = Tools.cleanEmail(authToken.getUsername());
		String password = authToken.getPassword();
		long captchaId = authToken.getCaptchaId();
		String captchaText = authToken.getCaptchaText();
	
		validateCaptcha(captchaId, captchaText);
		User user = validateUser(email, password);
	
		List<GrantedAuthority> authorities = getAuthorities(user);
	
		return new UsernamePasswordAuthenticationToken(email, password, authorities);
	}
	
	private void validateCaptcha(long captchaId, String captchaText) {
		Captcha captcha = captchaRepo.findById(captchaId).orElse(null);
		if (captcha == null) {
			throw new BadCredentialsException("Invalid captcha.");
		}
	
		captchaRepo.delete(captcha);
	
		if (!captcha.getText().equalsIgnoreCase(captchaText)) {
			throw new BadCredentialsException("Captcha text does not match.");
		}
	}
	
	private User validateUser(String email, String password) {
		User user = userRepo.findByEmail(email);
	
		if (user == null) {
			throw new BadCredentialsException("User not found.");
		}
	
		if (user.isDisabled()) {
			throw new DisabledException("User is disabled.");
		}
	
		if (password.isEmpty()) {
			throw new BadCredentialsException("Password is empty.");
		}
	
		if (!passwordEncoder.matches(password, user.getPassword())) {
			throw new BadCredentialsException("Invalid password.");
		}
	
		if (!user.isConfirmed() && !user.isAdmin()) {
			throw new InsufficientAuthenticationException("User is not confirmed.");
		}
	
		return user;
	}
	
	private List<GrantedAuthority> getAuthorities(User user) {
		List<GrantedAuthority> authorities = new ArrayList<>();
		String role = user.isAdmin() ? SecurityConfig.getRoleAdmin() : SecurityConfig.getRoleUser();
		authorities.add(new SimpleGrantedAuthority(role));
		return authorities;
	}
	

	@Override
	public boolean supports(Class<?> authentication) {
		return authentication.equals(UsernamePasswordAuthenticationToken.class)
				|| authentication.equals(AuthToken.class);
	}
}

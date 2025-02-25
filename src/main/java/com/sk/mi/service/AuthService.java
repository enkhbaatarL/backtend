package com.sk.mi.service;

import com.sk.mi.entity.master.Company;
import com.sk.mi.entity.master.Permission;
import com.sk.mi.entity.master.Role;
import com.sk.mi.entity.master.User;
import com.sk.mi.repository.master.CompanyRepository;
import com.sk.mi.repository.master.UserRepository;
import com.sk.mi.request.AuthRequest;
import com.sk.mi.request.SignupRequest;
import com.sk.mi.response.AuthResponse;
import com.sk.mi.util.JwtUtil;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final CompanyRepository companyRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;
//  private final TokenService tokenService;

  public String signUp(SignupRequest signupRequest) {
    if (userRepository.existsByUsername(signupRequest.getUsername())) {
      throw new IllegalArgumentException("Username is already taken.");
    }

    Company defaultCompany = companyRepository.findAll().stream()
        .findFirst()
        .orElseThrow(() -> new RuntimeException("No company found"));

    String encodedPassword = passwordEncoder.encode(signupRequest.getPassword());

    User user = User.builder()
        .username(signupRequest.getUsername())
        .password(encodedPassword)
        .role(Role.USER)
        .permissions(Set.of(Permission.REFINITIV_VIEW))
//        .permissions(Set.of(Permission.REFINITIV_VIEW, Permission.REFINITIV_DOWNLOAD))
        .company(defaultCompany)
        .build();

    userRepository.save(user);
    return "User registered successfully";
  }

  public AuthResponse login(AuthRequest authRequest) {
    User user = userRepository.findByUsername(authRequest.getUsername())
        .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

    if (!passwordEncoder.matches(authRequest.getPassword(), user.getPassword())) {
      throw new IllegalArgumentException("Invalid credentials");
    }

    String accessToken = jwtUtil.generateAccessToken(user);
    String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

    // TODO: refresh token을 레디스에 적재
//    tokenService.stroeRefreshToken(user.getUsername(), refreshToken);

    return new AuthResponse(accessToken, refreshToken);
  }

  public String refreshToken(String refreshToken) {
    if (jwtUtil.isTokenExpired(refreshToken)) {
      throw new IllegalArgumentException("Refresh token expired");
    }

    String username = jwtUtil.extractUsername(refreshToken);
    User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

    return jwtUtil.generateAccessToken(user);
  }

}

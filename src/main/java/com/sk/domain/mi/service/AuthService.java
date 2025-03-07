//package com.sk.domain.mi.service;
//
//import com.sk.domain.mi.dto.request.AuthReq;
//import com.sk.domain.mi.dto.response.AuthRes;
//import com.sk.common.utils.JwtUtil;
//import com.sk.domain.shared.entity.master.Company;
//import com.sk.domain.shared.enums.Permission;
//import com.sk.domain.shared.enums.Role;
//import com.sk.domain.shared.entity.master.User;
//import com.sk.domain.shared.repository.master.CompanyRepository;
//import com.sk.domain.shared.repository.master.UserRepository;
//import com.sk.domain.mi.dto.request.SignupReq;
//
//import java.util.Set;
//import lombok.RequiredArgsConstructor;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Service;
//
//@Service
//@RequiredArgsConstructor
//public class AuthService {
//
//  private final UserRepository userRepository;
//  private final CompanyRepository companyRepository;
//  private final PasswordEncoder passwordEncoder;
//  private final JwtUtil jwtUtil;
////  private final TokenService tokenService;
//
//  public String signUp(SignupReq signupReq) {
//    if (userRepository.existsByUsername(signupReq.getUsername())) {
//      throw new IllegalArgumentException("Username is already taken.");
//    }
//
//    Company defaultCompany = companyRepository.findAll().stream()
//        .findFirst()
//        .orElseThrow(() -> new RuntimeException("No company found"));
//
//    String encodedPassword = passwordEncoder.encode(signupReq.getPassword());
//
//    User user = User.builder()
//        .username(signupReq.getUsername())
//        .password(encodedPassword)
//        .role(Role.USER)
//        .permissions(Set.of(Permission.REFINITIV_VIEW))
////        .permissions(Set.of(Permission.REFINITIV_VIEW, Permission.REFINITIV_DOWNLOAD))
//        .company(defaultCompany)
//        .build();
//
//    userRepository.save(user);
//    return "User registered successfully";
//  }
//
//  public AuthRes login(AuthReq authReq) {
//    User user = userRepository.findByUsername(authReq.getUsername())
//        .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
//
//    if (!passwordEncoder.matches(authReq.getPassword(), user.getPassword())) {
//      throw new IllegalArgumentException("Invalid credentials");
//    }
//
//    String accessToken = jwtUtil.generateAccessToken(user);
//    String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());
//
//    // TODO: refresh token을 레디스에 적재
////    tokenService.stroeRefreshToken(user.getUsername(), refreshToken);
//
//    return new AuthRes(accessToken, refreshToken);
//  }
//
//  public String refreshToken(String refreshToken) {
//    if (jwtUtil.isTokenExpired(refreshToken)) {
//      throw new IllegalArgumentException("Refresh token expired");
//    }
//
//    String username = jwtUtil.extractUsername(refreshToken);
//    User user = userRepository.findByUsername(username)
//            .orElseThrow(() -> new IllegalArgumentException("User not found"));
//
//    return jwtUtil.generateAccessToken(user);
//  }
//
//}

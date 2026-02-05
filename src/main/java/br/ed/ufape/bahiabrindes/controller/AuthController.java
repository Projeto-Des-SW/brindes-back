package br.ed.ufape.bahiabrindes.controller;

import br.ed.ufape.bahiabrindes.dto.auth.LoginRequest;
import br.ed.ufape.bahiabrindes.dto.auth.LoginResponse;
import br.ed.ufape.bahiabrindes.dto.auth.ForgotPasswordRequest;
import br.ed.ufape.bahiabrindes.dto.auth.ResetPasswordRequest;
import br.ed.ufape.bahiabrindes.service.AuthService;
import br.ed.ufape.bahiabrindes.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Endpoints para autenticação de usuários")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @Autowired
    public AuthController(AuthService authService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/login")
    @Operation(
        summary = "Realizar login",
        description = "Autentica um usuário e retorna um token JWT válido por 24 horas"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Login realizado com sucesso",
            content = @Content(schema = @Schema(implementation = LoginResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Email ou senha inválidos",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Dados de entrada inválidos",
            content = @Content
        )
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Solicitar recuperação de senha", description = "Gera um token de 5 minutos e envia por email")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Solicitação processada"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content)
    })
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Resetar senha", description = "Valida o token e define a nova senha")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Senha alterada"),
        @ApiResponse(responseCode = "400", description = "Token inválido/expirado ou dados inválidos", content = @Content)
    })
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok().build();
    }
}

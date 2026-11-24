package com.library.user_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users", uniqueConstraints = {
        // matricula (aluno) ou MASP (professor): unico dentro do tipo
        @UniqueConstraint(name = "uk_users_type_registration_number", columnNames = {"type", "registration_number"})
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    // Nulo apenas para LIBRARIAN
    @Enumerated(EnumType.STRING)
    private Usertype type;

    // Hash BCrypt. Nulo para membro cadastrado no balcao que ainda nao ativou o acesso.
    private String password;

    // Opcional no banco para nao quebrar registros antigos; obrigatorio na aplicacao
    @Column(length = 11, unique = true)
    private String cpf;

    // O default evita que linhas antigas recebam outro papel quando a coluna for criada
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(20) not null default 'MEMBER'")
    private Role role = Role.MEMBER;

    @Column(name = "registration_number")
    private String registrationNumber;
}

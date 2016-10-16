package mook;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationData {
    private int id;
    private String email;
    private String displayName;
    private String token;
}

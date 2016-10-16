package mook;

import java.security.Principal;

public class MookPrincipal implements Principal {

    private final int id;

    private final String email;

    private final String displayName;

    MookPrincipal(int id, String email, String displayName) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
    }

    @Override
    public String getName() {
        return email;
    }

    public int getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }
}
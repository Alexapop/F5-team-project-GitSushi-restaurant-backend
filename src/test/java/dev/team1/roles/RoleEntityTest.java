package dev.team1.roles;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.team1.users.UserEntity;

public class RoleEntityTest {


    @Test 
    void testRoleEntity() {

        RoleEntity roleEmpty = new RoleEntity();

        UserEntity admin = new UserEntity();
        admin.setEmail("admin@gitsushi.com");
        List<UserEntity> users = List.of(admin);

        RoleEntity role = new RoleEntity();
        role.setName("ROLE_ADMIN");
        role.setUsers(users);
        
        assertThat(roleEmpty.getName(), is(equalTo(null)));
        assertThat(role.getName(), is(equalTo("ROLE_ADMIN")));
        assertThat(role.getUsers().get(0).getEmail(), is(equalTo("admin@gitsushi.com")));
    }

}

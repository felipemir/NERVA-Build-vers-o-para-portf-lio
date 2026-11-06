package com.nervatech.nerva_build.security;

import com.nervatech.nerva_build.domain.model.CustomerAccount;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomerAccountDetails implements UserDetails {

    private final CustomerAccount customerAccount;

    public CustomerAccountDetails(CustomerAccount customerAccount) {
        this.customerAccount = customerAccount;
    }

    public Long getId() {
        return customerAccount.getId();
    }

    public String getDisplayName() {
        return customerAccount.getName();
    }

    public String getDocument() {
        return customerAccount.getDocument();
    }

    public String getPhone() {
        return customerAccount.getPhone();
    }

    public String getPersonType() {
        return customerAccount.getPersonType();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    @Override
    public String getPassword() {
        return customerAccount.getPassword();
    }

    @Override
    public String getUsername() {
        return customerAccount.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(customerAccount.getActive());
    }
}

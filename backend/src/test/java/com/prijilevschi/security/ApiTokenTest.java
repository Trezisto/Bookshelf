package com.prijilevschi.security;

import com.prijilevschi.SqliteTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "library.api-token=" + ApiTokenTest.TOKEN)
@AutoConfigureMockMvc
class ApiTokenTest extends SqliteTestSupport {

    static final String TOKEN = "test-token-0123456789";

    @Autowired
    MockMvc mvc;

    @Test
    void apiNeedsTheBearerToken() throws Exception {
        mvc.perform(get("/api/shelves"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));
        mvc.perform(get("/api/shelves").header(HttpHeaders.AUTHORIZATION, "Bearer wrong-token-0123456789"))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/books/1")).andExpect(status().isUnauthorized());

        mvc.perform(get("/api/shelves").header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(status().isOk());
        mvc.perform(get("/api/shelves").header(HttpHeaders.AUTHORIZATION, "bearer " + TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void apiDocsStayReadableWithoutToken() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    void startupRefusesAMissingOrShortTokenUnlessAnonymousIsAllowed() {
        SecurityConfig config = new SecurityConfig();
        assertThatThrownBy(() -> config.apiTokenFilter("", false))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("LIBRARY_API_TOKEN");
        assertThatThrownBy(() -> config.apiTokenFilter("short", false))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("at least");
        config.apiTokenFilter("", true);
    }
}

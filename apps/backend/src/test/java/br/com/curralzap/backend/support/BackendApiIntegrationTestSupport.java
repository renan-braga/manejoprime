package br.com.curralzap.backend.support;

import br.com.curralzap.backend.auth.application.AuthTokenPair;
import br.com.curralzap.backend.auth.application.AuthTokenPairIssuer;
import br.com.curralzap.backend.users.domain.UserAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;

@AutoConfigureMockMvc
public abstract class BackendApiIntegrationTestSupport extends BackendIntegrationTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected AuthTokenPairIssuer authTokenPairIssuer;

    protected String issueAccessToken(UserAccount userAccount, OffsetDateTime issuedAt) {
        AuthTokenPair tokenPair = authTokenPairIssuer.issueFor(
                userAccount,
                UUID.randomUUID(),
                UUID.randomUUID(),
                issuedAt,
                issuedAt.plusDays(30)
        );
        return tokenPair.accessToken();
    }
}

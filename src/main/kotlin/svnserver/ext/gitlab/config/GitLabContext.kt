/*
 * This file is part of git-as-svn. It is subject to the license terms
 * in the LICENSE file found in the top-level directory of this distribution
 * and at http://www.gnu.org/licenses/gpl-2.0.html. No part of git-as-svn,
 * including this file, may be copied, modified, propagated, or distributed
 * except according to the terms contained in the LICENSE file.
 */
package svnserver.ext.gitlab.config

import com.google.api.client.auth.oauth.OAuthGetAccessToken
import com.google.api.client.auth.oauth2.PasswordTokenRequest
import com.google.api.client.auth.oauth2.TokenRequest
import com.google.api.client.auth.oauth2.TokenResponseException
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import org.gitlab4j.api.Constants.TokenType
import org.gitlab4j.api.GitLabApi
import org.gitlab4j.api.GitLabApi.ApiVersion
import org.gitlab4j.api.GitLabApiException
import org.gitlab4j.api.utils.AccessTokenUtils
import org.gitlab4j.api.utils.AccessTokenUtils.Scope
import svnserver.context.Shared
import svnserver.context.SharedContext
import java.io.IOException
import java.net.HttpURLConnection

/**
 * GitLab context.
 *
 * @author Artem V. Navrotskiy <bozaro@users.noreply.github.com>
 */
class GitLabContext internal constructor(val config: GitLabConfig) : Shared {
    @Throws(IOException::class)
    fun connect(username: String, password: String): GitLabApi {
        val token = obtainAccessToken(gitLabUrl, username, password, false)
        val api = connect(gitLabUrl, token)
        return api
    }

    fun connect(): GitLabApi {
        return Companion.connect(gitLabUrl, token)
    }

    val gitLabUrl: String
        get() = config.url
    val token: GitLabToken
        get() = config.getToken()
    val hookPath: String
        get() = config.hookPath

    companion object {
        private val transport: HttpTransport = NetHttpTransport()
        fun sure(context: SharedContext): GitLabContext {
            return context.sure(GitLabContext::class.java)
        }

        @Throws(IOException::class)
        fun obtainAccessToken(gitlabUrl: String, username: String, password: String, sudoScope: Boolean): GitLabToken {
            return try {
                val tokenServerUrl = OAuthGetAccessToken(gitlabUrl + "/oauth/token?scope=api" + if (sudoScope) "%20sudo" else "")
                val oauthResponse = PasswordTokenRequest(transport, GsonFactory.getDefaultInstance(), tokenServerUrl, username, password).execute()
                GitLabToken(TokenType.OAUTH2_ACCESS, oauthResponse.accessToken)
            } catch (e: TokenResponseException) {
                if (sudoScope && e.statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    // Fallback for pre-10.2 gitlab versions
                    val tokenServerUrl = OAuthGetAccessToken(gitlabUrl + "/api/v3/session")
                    val oauthResponse = TokenRequest(transport, GsonFactory.getDefaultInstance(), tokenServerUrl, "password")
                        .set("login", username)
                        .set("password", password)
                        .execute()
                    GitLabToken(TokenType.PRIVATE, oauthResponse.get("private_token").toString())
                } else {
                    throw GitLabApiException(e.message, e.statusCode)
                }
            }
        }

        fun connect(gitlabUrl: String, token: GitLabToken): GitLabApi {
            return GitLabApi(gitlabUrl, token.type, token.value)
        }
    }
}

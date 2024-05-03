/*
 * This file is part of git-as-svn. It is subject to the license terms
 * in the LICENSE file found in the top-level directory of this distribution
 * and at http://www.gnu.org/licenses/gpl-2.0.html. No part of git-as-svn,
 * including this file, may be copied, modified, propagated, or distributed
 * except according to the terms contained in the LICENSE file.
 */
package svnserver.ext.gitlab.auth

import org.gitlab4j.api.Constants.TokenType
import svnserver.auth.UserDB
import svnserver.config.UserDBConfig
import svnserver.context.SharedContext
import svnserver.ext.gitlab.config.GitLabContext
import svnserver.ext.gitlab.config.GitLabToken

/**
 * GitLab authentication configuration.
 *
 * @author Artem V. Navrotskiy <bozaro@users.noreply.github.com>
 */
class GitLabUserDBConfig : UserDBConfig {
    val authentication: GitlabAuthentication = GitlabAuthentication.Password

    override fun create(context: SharedContext): UserDB {
        return GitLabUserDB(this, context)
    }
}

enum class GitlabAuthentication {
    Password {
        override fun obtainAccessToken(gitLabUrl: String, username: String, password: String): GitLabToken {
            return GitLabContext.obtainAccessToken(gitLabUrl, username, password, false)
        }
    },
    AccessToken {
        override fun obtainAccessToken(gitLabUrl: String, username: String, password: String): GitLabToken {
            return GitLabToken(TokenType.ACCESS, password)
        }
    },
    PrivateToken {
        override fun obtainAccessToken(gitLabUrl: String, username: String, password: String): GitLabToken {
            return GitLabToken(TokenType.PRIVATE, password)
        }
    };

    abstract fun obtainAccessToken(gitLabUrl: String, username: String, password: String): GitLabToken
}

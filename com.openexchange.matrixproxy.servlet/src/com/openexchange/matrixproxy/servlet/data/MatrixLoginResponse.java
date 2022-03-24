
package com.openexchange.matrixproxy.servlet.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * {@link MatrixLoginResponse}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MatrixLoginResponse {

    private String user_id;
    private String access_token;

    public String getUser_id() {
        return user_id;
    }

    public String getAccess_token() {
        return access_token;
    }

    @Override
    public String toString() {
        return "MatrixLoginResponse [user_id=" + user_id + ", access_token=" + access_token + "]";
    }

}

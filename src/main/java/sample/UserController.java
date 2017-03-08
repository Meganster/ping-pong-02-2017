package sample;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;

/**
 * Created by sergey on 24.02.17.
 */

@CrossOrigin    //Enables CORS from all origins
@RestController
public class UserController {
    @NotNull
    private final AccountService accountService;
    private final ObjectMapper mapper = new ObjectMapper();


    @PostMapping(path = "/api/user/registration")
    public ObjectNode register(@RequestBody UserProfile body, HttpSession httpSession, HttpServletResponse response) {
        ObjectNode responseJSON = mapper.createObjectNode();
        final ArrayNode errorList = mapper.createArrayNode();
        if(isEmptyField(body.getEmail())) {
            errorList.add(getEmptyFieldError("email"));
        }

        if(isEmptyField(body.getPassword())) {
            errorList.add(getEmptyFieldError("password"));
        }

        if(isEmptyField(body.getLogin())) {
            errorList.add(getEmptyFieldError("login"));
        }

        if (errorList.size() > 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            responseJSON.set("errors", errorList);
            return responseJSON;
        }

        final UserProfile userProfile = accountService.register(body.getEmail(), body.getLogin(), body.getPassword());
        if(userProfile != null) {
            responseJSON = userProfileToJSON(userProfile);
            httpSession.setAttribute("email", body.getEmail());
        } else {
            responseJSON.put("error", "this email is occupied");
            response.setStatus(HttpServletResponse.SC_CONFLICT);
        }
        return responseJSON;
    }

    @PostMapping(path = "/api/user/login")
    public ObjectNode login(@RequestBody UserProfile body, HttpSession httpSession, HttpServletResponse response) {
        final ObjectNode responseJSON = mapper.createObjectNode();
        final ArrayNode errorList = mapper.createArrayNode();

        if(isEmptyField(body.getEmail())) {
            errorList.add(getEmptyFieldError("email"));
        }

        if(isEmptyField(body.getPassword())) {
            errorList.add(getEmptyFieldError("password"));
        }

        if (errorList.size() > 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            responseJSON.set("error", errorList);
            return responseJSON;
        }

        if(accountService.login(body.getEmail(), body.getPassword())) {
            httpSession.setAttribute("email", body.getEmail());
            responseJSON.put("status", "success");
        }
        else {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            responseJSON.put("error", "invalid email or password");
        }
        return responseJSON;
    }

    @PostMapping(path = "/api/user/logout")
    public ObjectNode logout(HttpSession httpSession, HttpServletResponse response) {
        final ObjectNode responseJSON = mapper.createObjectNode();
        if(httpSession.getAttribute("email") != null) {
            responseJSON.put("status", "success");
            httpSession.invalidate();
        } else {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            responseJSON.put("error", "user didn\'t login");
        }
        return responseJSON;
    }

    @GetMapping(path = "/api/user/getuser")
    public ObjectNode getUser(HttpSession httpSession , HttpServletResponse response) {
        ObjectNode responseJSON = mapper.createObjectNode();
        if(httpSession.getAttribute("email") != null) {
            final UserProfile userProfile = accountService.getUser(httpSession.getAttribute("email").toString());
            responseJSON = userProfileToJSON(userProfile);
        } else {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            responseJSON.put("error", "user didn\'t login");
        }
        return responseJSON;
    }


    @PostMapping(path = "/api/user/update")
    public ObjectNode updateUser(@RequestBody UserProfile changedUserProfile, HttpSession httpSession, HttpServletResponse response) {
        ObjectNode responseJSON = mapper.createObjectNode();

        if(httpSession.getAttribute("email") != null) {
            if(!isEmptyField(changedUserProfile.getEmail()) && !isEmptyField(changedUserProfile.getPassword()) &&
                    !isEmptyField(changedUserProfile.getLogin())) {
                final UserProfile oldUserProfile = accountService.getUser(httpSession.getAttribute("email").toString());
                final UserProfile updatedUserProfile = accountService.update(oldUserProfile, changedUserProfile);

                if (updatedUserProfile == null) {
                    response.setStatus(HttpServletResponse.SC_CONFLICT);
                    responseJSON.put("error", "this email is occupied");
                    return responseJSON;
                }

                httpSession.setAttribute("email", updatedUserProfile.getEmail());
                responseJSON = userProfileToJSON(changedUserProfile);
                return responseJSON;
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                responseJSON.put("error", "data to update is empty");
            }
        } else {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            responseJSON.put("error", "user didn't login");
        }
        return responseJSON;
    }


    @PostMapping(path = "/api/user/score")
    public ObjectNode setScore(@RequestBody ObjectNode score, HttpSession httpSession, HttpServletResponse response) {
        final ObjectNode responseJSON = mapper.createObjectNode();
        if(isEmptyField(score.get("score").toString())) {
            responseJSON.put("error", getEmptyFieldError("score"));
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return responseJSON;
        }
        if(httpSession.getAttribute("email") != null) {
            final UserProfile userProfile = accountService.getUser(httpSession.getAttribute("email").toString());
            userProfile.setScore(score.get("score").intValue());
            accountService.updateScore(userProfile);
            responseJSON.put("status", "success");
        } else {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            responseJSON.put("error", "user didn\'t login");
        }
        return responseJSON;
    }

    @PostMapping(path = "/api/user/leaders")
    public ObjectNode getLeaders(@RequestBody ObjectNode countJSON, HttpSession httpSession , HttpServletResponse response) {
        final ObjectNode responseJSON = mapper.createObjectNode();
        int usersCounter;
        if(countJSON.get("count") == null) {
            usersCounter = 1;
        } else {
            usersCounter = countJSON.get("count").intValue();
        }
        final ArrayNode leadersList = mapper.createArrayNode();
        final ArrayList<UserProfile> userProfileArrayList = accountService.getSortedUsersByScore();
        for(int counter = 0; counter < userProfileArrayList.size() && counter < usersCounter; counter++) {
            leadersList.add(userProfileToJSON(userProfileArrayList.get(counter)));
        }
        responseJSON.set("leaders",leadersList);
        return responseJSON;
    }

    @GetMapping(path = "/api/user/islogin")
    public ObjectNode isLogin(HttpSession httpSession, HttpServletResponse response) {
        final ObjectNode responseJSON = mapper.createObjectNode();
        if(httpSession.getAttribute("email") != null) {
            responseJSON.put("isLoggedIn","true");
        } else {
            responseJSON.put("isLoggedIn","false");
        }
        return responseJSON;
    }

    @GetMapping(path = "/api/user/flush")
    public void flush() {
        accountService.flush();
    }


    public ObjectNode userProfileToJSON (UserProfile userProfile) {
        final ObjectNode userProfileJSON = mapper.createObjectNode();

        userProfileJSON.put("id", userProfile.getId());
        userProfileJSON.put("login", userProfile.getLogin());
        userProfileJSON.put("email", userProfile.getEmail());
        userProfileJSON.put("score", userProfile.getScore());
        return userProfileJSON;
    }

    private boolean isEmptyField(String field) {
        return ((field == null) || field.isEmpty());
    }

    private String getEmptyFieldError(String fieldName) {
        return ("field " + fieldName + " is empty");
    }

    public UserController(@NotNull AccountService accountService) {
        this.accountService = accountService;
    }

}




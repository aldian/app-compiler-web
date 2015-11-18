package com.aldianfazrihady;

import com.aldianfazrihady.api.AppCompilerWSAPI;
import com.aldianfazrihady.controller.WebContent;
import com.aldianfazrihady.controller.WebServices;
import com.aldianfazrihady.model.CompilationResult;
import com.aldianfazrihady.model.User;
import com.aldianfazrihady.security.SecurityUser;
import com.aldianfazrihady.service.CompilationResultService;
import com.aldianfazrihady.service.UserService;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = AppCompilerWebApplication.class)
@WebAppConfiguration
public class AppCompilerWebApplicationTests {
    private static final String TEST_USERNAME = "aldian";
    private static final String TEST_PASSWORD = "password";
    private static final String HELLO_WORLD_CODE = "public class Hello { public static void main(String[] args) { System.out.println(\"Hello Java World!\"); }}";
    private static final String HELLO_WORLD_CODE_ERROR_01 = "public class Hello { public static void main(String[] args) { System.out.printl(\"Hello Java World!\"); }}";
    private static final String HELLO_WORLD_RESULT_SUCCESS = "javac Hello.java\n";
    private static final String HELLO_WORLD_RESULT_FAILED_01 = "System.out.printl(";

    @Value("${app.ws.host}")
    private String wsHost;

    @Value("${app.ws.port}")
    private int wsPort;

    @Autowired
    private UserService userService;

    @Autowired
    private CompilationResultService compilationResultService;

    @InjectMocks
    private WebServices ws;

    @InjectMocks private WebContent web;
    //@Mock(name = "userService") private UserService webUserService;

    @Mock
    private View mockView;

    private MockMvc mockWs;
    private MockMvc mockWeb;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        ReflectionTestUtils.setField(ws, "userService", userService);
        ReflectionTestUtils.setField(ws, "compilationResultService", compilationResultService);

        ReflectionTestUtils.setField(web, "userService", userService);
        ReflectionTestUtils.setField(web, "compilationResultService", compilationResultService);
        ReflectionTestUtils.setField(web, "wsHost", wsHost);
        ReflectionTestUtils.setField(web, "wsPort", wsPort);

        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/templates/");
        viewResolver.setSuffix(".html");

        mockWs = MockMvcBuilders.standaloneSetup(ws).build();
        mockWeb = MockMvcBuilders.standaloneSetup(web).setViewResolvers(viewResolver).build();
    }

    @Test
    public void testWsLogin() throws Exception {
        ResultActions resActions = mockWs.perform(MockMvcRequestBuilders.post("/ws/login").param("username", TEST_USERNAME).param("password", TEST_PASSWORD));
        MvcResult res = resActions.andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String token = res.getResponse().getContentAsString();
        assert(token.length() > 30 && token.length() < 100);
    }

    @Test
    public void testWsLogout() throws Exception {
        ResultActions resActions = mockWs.perform(MockMvcRequestBuilders.post("/ws/login").param("username", TEST_USERNAME).param("password", TEST_PASSWORD));
        MvcResult res = resActions.andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String token = res.getResponse().getContentAsString();

        resActions = mockWs.perform(MockMvcRequestBuilders.post("/ws/logout").param("accessToken", token));
        resActions.andExpect(MockMvcResultMatchers.status().isOk());

        User user = userService.findByUsername(TEST_USERNAME);
        assert(user.getWsToken() == null);
    }

    private MockMultipartFile getMockZipFile(String fileParamName, String javaCode) throws ArchiveException, IOException {
        ByteArrayOutputStream zipBytes = new ByteArrayOutputStream();
        ArchiveOutputStream zippedOut = new ArchiveStreamFactory().createArchiveOutputStream(ArchiveStreamFactory.ZIP, zipBytes);
        ZipArchiveEntry arcEntry = new ZipArchiveEntry("Hello.java");
        arcEntry.setSize(javaCode.getBytes().length);
        zippedOut.putArchiveEntry(arcEntry);
        zippedOut.write(javaCode.getBytes());
        zippedOut.closeArchiveEntry();
        zippedOut.close();

        byte[] zipByteArray = zipBytes.toByteArray();
        return new MockMultipartFile(fileParamName, "kucing.zip", "application/zip", zipByteArray);
    }

    @Test
    public void testWsCompile() throws Exception {
        ResultActions resActions = mockWs.perform(MockMvcRequestBuilders.post("/ws/login").param("username", TEST_USERNAME).param("password", TEST_PASSWORD));
        MvcResult res = resActions.andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String token = res.getResponse().getContentAsString();

        MockMultipartFile mockFile = getMockZipFile("zipFile", HELLO_WORLD_CODE);
        resActions = mockWs.perform(MockMvcRequestBuilders.fileUpload("/ws/compile").file(mockFile).param("accessToken", token));
        res = resActions.andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String compilationResult = res.getResponse().getContentAsString();
        JSONObject jsonObj = new JSONObject(compilationResult);
        String message = (String) jsonObj.get("message");
        assert(message.equals(HELLO_WORLD_RESULT_SUCCESS));
    }

    @Test
	public void testWsApiLogin() throws IOException {
        AppCompilerWSAPI api = new AppCompilerWSAPI(wsHost, wsPort);
        String token = api.login(TEST_USERNAME, TEST_PASSWORD);
        assert(token != null);
    }

    @Test
    public void testWsApiLogout() throws IOException {
        AppCompilerWSAPI api = new AppCompilerWSAPI(wsHost, wsPort);
        String token = api.login(TEST_USERNAME, TEST_PASSWORD);
        assert(token != null);
        api.logout();
        User user = userService.findByUsername(TEST_USERNAME);
        assert(user.getWsToken() == null);
    }

    @Test
    public void testWsApiCompile() throws IOException, ArchiveException {
        AppCompilerWSAPI api = new AppCompilerWSAPI(wsHost, wsPort);
        String token = api.login(TEST_USERNAME, TEST_PASSWORD);
        assert(token != null);

        ByteArrayOutputStream zipBytes = new ByteArrayOutputStream();
        ArchiveOutputStream zippedOut = new ArchiveStreamFactory().createArchiveOutputStream(ArchiveStreamFactory.ZIP, zipBytes);
        ZipArchiveEntry arcEntry = new ZipArchiveEntry("Hello.java");
        arcEntry.setSize(HELLO_WORLD_CODE.getBytes().length);
        zippedOut.putArchiveEntry(arcEntry);
        zippedOut.write(HELLO_WORLD_CODE.getBytes());
        zippedOut.closeArchiveEntry();
        zippedOut.close();
        byte[] zipByteArray = zipBytes.toByteArray();
        String compilationResult = api.compile(zipByteArray);
        JSONObject jsonObj = new JSONObject(compilationResult);
        assert(jsonObj.get("message").equals(HELLO_WORLD_RESULT_SUCCESS));
    }

    @Test
    public void testWebHandleFileUpload() throws Exception {
        Authentication mockTestUserAuth = Mockito.mock(Authentication.class);
        //User testUser = userService.findByUsername(TEST_USERNAME);
        //Mockito.when(webUserService.findByUsername(TEST_USERNAME)).thenReturn(testUser);
        //Mockito.when(webUserService.generateWebServiceToken(testUser)).thenReturn(userService.generateWebServiceToken(testUser));
        Mockito.when(mockTestUserAuth.getPrincipal()).thenReturn(new SecurityUser(new User(TEST_USERNAME, TEST_PASSWORD)));
        SecurityContextHolder.getContext().setAuthentication(mockTestUserAuth);
        MockHttpSession session = new MockHttpSession();
        MockMultipartFile mockFile = getMockZipFile("file", HELLO_WORLD_CODE_ERROR_01);

        ResultActions resActions = mockWeb.perform(MockMvcRequestBuilders.fileUpload("/upload").file(mockFile).session(session));

        MvcResult res = resActions.andExpect(MockMvcResultMatchers.status().is3xxRedirection()).andReturn();
        long logId = (Long) session.getAttribute("logId");
        assert(logId > 0);
        String redirLoc = res.getResponse().getHeader("Location");
        assert(redirLoc.equals("/"));
        String responseBody = res.getResponse().getContentAsString();
        assert(responseBody.equals(""));

        CompilationResult compilationResult = compilationResultService.findById(logId);
        assert(compilationResult.getMessage().contains(HELLO_WORLD_RESULT_FAILED_01));
    }

    @Test
    public void testWebRegistrationPost() throws Exception {
        ResultActions resActions = mockWeb.perform(MockMvcRequestBuilders.post("/registration").param("username", TEST_USERNAME).param("password", TEST_PASSWORD));
        resActions.andExpect(MockMvcResultMatchers.status().is2xxSuccessful());

        String newUsername = "newuser123";
        User newUser = userService.findByUsername(newUsername);
        if (newUser != null) {
            userService.deleteUser(newUser.getId());
        }
        resActions = mockWeb.perform(MockMvcRequestBuilders.post("/registration").param("username", newUsername).param("password", TEST_PASSWORD));
        resActions.andExpect(MockMvcResultMatchers.status().is3xxRedirection());

        newUser = userService.findByUsername(newUsername);
        assert(newUser != null);
    }
}

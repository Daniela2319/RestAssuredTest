package Pets.User;

import Entities.User;
import com.github.javafaker.Faker;
import io.restassured.RestAssured;
import io.restassured.config.LogConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static io.restassured.config.LogConfig.logConfig;
import static org.hamcrest.Matchers.*;
import static io.restassured.module.jsv.JsonSchemaValidator.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class) //ordem dos testes
public class UserTest {
    private static User user;
    public static Faker faker;
    public static RequestSpecification request;

    @BeforeAll
    public static void setup(){
        RestAssured.baseURI = "https://petstore.swagger.io/v2";

        faker = new Faker();
        user = new User(faker.name().username(),
                faker.name().firstName(),
                faker.name().lastName(),
                faker.internet().safeEmailAddress(),
                faker.internet().password(8,10),
                faker.phoneNumber().toString());
    }
    @BeforeEach
    public void setRequest(){
        request = given().config(RestAssured.config().logConfig(logConfig().enableLoggingOfRequestAndResponseIfValidationFails()))
                .header("api-key","special-key")
                .contentType(ContentType.JSON);
    }

    @Test
    @Order(1)
    public void CriarNovoUsuario_ComRetornoDadosValidos(){
        request
                .body(user)
                .when()
                .post("/user")
                .then()
                .assertThat().statusCode(200).and()
                .body("code", equalTo(200))
                .body("type", equalTo("unknown"))
                .body("message", isA(String.class))
                .body("size()", equalTo(3));

    }
    @Test
    @Order(4)
    public void ObterLogin_UsuarioValido_Retorna(){
        request
                .param("username", user.getUsername())
                .param("password", user.getPassword())
                .when()
                .get("/user/login")
                .then()
                .assertThat()
                .statusCode(200)
                .and().time(lessThan(2000L))
                .and().body(matchesJsonSchemaInClasspath("LoginResponseSchema.json"));
    }
    @Test
    @Order(3)
    public void ObterUsuarioPorNomeUsuario_UsarioValido(){
        request
                .when()
                .get("/user/" + user.getUsername())
                .then()
                .assertThat().statusCode(200)
                .and().time(lessThan(2000L))
                .and().body("firstName", equalTo(user.getFirstName()));
    }

    @Test
    @Order(3)
    public void DeleteUsuario_UsuarioExiste_RetornaOK(){

        request
                .when()
                .delete("/user" + user.getUsername())
                .then()
                .assertThat()
                .statusCode(200)
                .and().time(lessThan(2000L))
                .log();

    }
    @Test
    @Order(5)
    public void criarNovoUsuario_CorpoInvalido_retornaSolicitacaoInvalida(){

        Response response = request
                .body("teste")
                .when()
                .post("/user")
                .then()
                .extract()
                .response();

        Assertions.assertNotNull(response);
        Assertions.assertEquals(400, response.statusCode());
        Assertions.assertEquals(true, response.getBody().asPrettyString().contains("unknown"));
        Assertions.assertEquals(3,response.body().jsonPath().getMap("$").size());
    }

}

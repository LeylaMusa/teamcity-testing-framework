

import com.codeborne.selenide.Condition;
import com.example.teamcity.api.models.Project;
import com.example.teamcity.api.models.BuildType;
import com.example.teamcity.api.models.User; // Add User import if needed
import com.example.teamcity.api.generators.TestDataGenerator;
import com.example.teamcity.ui.pages.CreateBuildPage;
import com.example.teamcity.ui.pages.ProjectPage;
import org.testng.annotations.Test;

import static io.qameta.allure.Allure.step;

@Test(groups = {"Regression"})
public class CreateBuildTest extends BaseUiTest {

    private static final String REPO_URL = "https://github.com/AlexPshe/spring-core-for-qa";

    @Test(description = "User should be able to create build", groups = {"Positive"})
    public void userCreatesBuild() {
        Project project = TestDataGenerator.generate(Project.class);
        BuildType buildType = TestDataGenerator.generate(BuildType.class);
        User user = new User("testuser", "password123"); // Or use testData.getUser() if available
        loginAs(user);
        String createBuildUrl = "http://localhost:8111/admin/createObjectMenu.html?projectId=" + project.getId() + "&showMode=createBuildTypeMenu";
        CreateBuildPage.open(createBuildUrl)
                .createBuild(buildType.getName(), REPO_URL);

        var createdBuild = superUserCheckRequests.<BuildType>getRequest(Endpoint.BUILD_TYPES)
                .read("name:" + buildType.getName() + " projectId:" + project.getId());
        softy.assertNotNull(createdBuild);

        ProjectPage.open(project.getId())
                .buildList.shouldHave(Condition.text(buildType.getName()));
    }

    @Test(description = "User should not be able to create build without name", groups = {"Negative"})
    public void userCreatesBuildWithoutName() {
        Project project = TestDataGenerator.generate(Project.class);

        User user = new User("testuser", "password123"); // Or use testData.getUser() if available

        loginAs(user);

        String createBuildUrl = "http://localhost:8111/admin/createObjectMenu.html?projectId=" + project.getId() + "&showMode=createBuildTypeMenu";
        CreateBuildPage.open(createBuildUrl)
                .createBuild("", REPO_URL); // Simulate missing build name

        step("Verify error message that build name is required.");
        CreateBuildPage.getErrorMessage().shouldHave(Condition.text("Build name must not be empty"));

        var createdBuild = superUserCheckRequests.<BuildType>getRequest(Endpoint.BUILD_TYPES)
                .read("name: no-name projectId:" + project.getId());
        softy.assertNull(createdBuild);
    }
}

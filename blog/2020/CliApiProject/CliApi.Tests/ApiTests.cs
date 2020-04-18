namespace CliApi.Tests
{
    using System.Net;
    using System.Net.Http;
    using System.Threading.Tasks;
    using Xunit;

    public class ApiTests : ApiTestBase<Startup>
    {
        private readonly HttpClient _client;

        public ApiTests()
        {
            _client = CreateClient();
        }

        [Fact]
        public async Task Can_Get_From_Any_Url()
        {
            HttpResponseMessage response = await _client.GetAsync(string.Empty);
            Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        }
    }
}
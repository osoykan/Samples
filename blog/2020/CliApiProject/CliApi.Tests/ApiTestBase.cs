namespace CliApi.Tests
{
    using System;
    using System.Collections.Generic;
    using System.Linq;
    using Microsoft.AspNetCore.Mvc.Testing;
    using Microsoft.Extensions.Configuration;
    using Microsoft.Extensions.DependencyInjection;
    using Microsoft.Extensions.Hosting;

    public abstract class ApiTestBase<TEntryPoint> : WebApplicationFactory<TEntryPoint>
        where TEntryPoint : class
    {
        protected Dictionary<string, string> Configuration { get; set; }

        protected Action<IServiceCollection> ConfigureServicesForTest { get; set; }

        protected override IHostBuilder CreateHostBuilder()
        {
            IHostBuilder builder = base.CreateHostBuilder();
            builder.ConfigureServices(collection =>
            {
                //Configure test services
                ConfigureServicesForTest?.Invoke(collection);
            });
            builder.ConfigureAppConfiguration((context, configBuilder) =>
            {
                if (Configuration != null && Configuration.Any())
                {
                    configBuilder.AddInMemoryCollection(Configuration);
                }
            });
            return builder;
        }
    }
}
using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Hosting;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;

public class Startup
{
    public Startup(IConfiguration configuration)
    {
        Configuration = configuration;
    }

    public IConfiguration Configuration { get; }

    public void ConfigureServices(IServiceCollection services)
    {
        services.AddMvc();

        //var objectMapper = new MapsterObjectMapper();

        //var bus = new InMemoryBus();
        //var handler = new ProductAppService(new ProductDomainService(new ProductRepository(bus)));
        //bus.Register<CreateProductCommand>(command => handler.Handle(command));
        //var repository = new ProductRepository(bus);

        //services.AddTransient<IProductReadModel>(provider => new ProductReadModel(repository, objectMapper));
        //services.AddSingleton<IBus>(provider => bus);
    }

    public void Configure(IApplicationBuilder app, IHostingEnvironment env)
    {
        if (env.IsDevelopment())
        {
            app.UseDeveloperExceptionPage();
        }

        app.UseMvc();
    }
}

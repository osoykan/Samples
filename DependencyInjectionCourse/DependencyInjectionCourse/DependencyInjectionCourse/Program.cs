using System;
using System.Security.Claims;
using System.Threading;

using Autofac;
using Autofac.Extras.DynamicProxy;

using Castle.Core.Logging;
using Castle.DynamicProxy;

using DependencyInjectionCourse.Interceptors;

namespace DependencyInjectionCourse
{
    public class Program
    {
        private static void Main(string[] args)
        {
            var builder = new ContainerBuilder();

            builder.RegisterType<ConsoleLogger>().As<ILogger>().PropertiesAutowired(PropertyWiringOptions.AllowCircularDependencies);

            builder.RegisterType<CacheManager>().As<ICacheManager>();
            builder.RegisterType<CacheStarter>().As<IStartable>();

            builder.RegisterType<OrderService>()
                   .As<IOrderService>()
                   .EnableClassInterceptors(ProxyGenerationOptions.Default, typeof(IOrderService))
                   .InterceptedBy(typeof(SecurityInterceptor));

            builder.RegisterType<SecurityInterceptor>().PropertiesAutowired(PropertyWiringOptions.AllowCircularDependencies);

            IContainer container = builder.Build();

            var claims = Thread.CurrentPrincipal as ClaimsPrincipal;
            claims.AddIdentity(new ClaimsIdentity(new Claim[] { new Claim(ClaimTypes.Role, "User") }));

            var orderService = container.Resolve<IOrderService>();
            OrderResult result = orderService.DoOrder(1);

            Console.ReadKey();
        }
    }
}

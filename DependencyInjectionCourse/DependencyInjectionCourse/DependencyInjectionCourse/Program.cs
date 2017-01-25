using System;
using System.Security.Claims;
using System.Threading;

using Autofac;
using Autofac.Extras.DynamicProxy;
using Autofac.Features.ResolveAnything;

using Castle.DynamicProxy;

using DependencyInjectionCourse.Cache;
using DependencyInjectionCourse.Interceptors;
using DependencyInjectionCourse.Logger;
using DependencyInjectionCourse.Order;

namespace DependencyInjectionCourse
{
    public class Program
    {
        /// <summary>
        ///     Register Resolve Release !
        /// </summary>
        /// <param name="args">The arguments.</param>
        private static void Main(string[] args)
        {
            //COMPOSITION ROOT !

            var builder = new ContainerBuilder();
            builder.RegisterSource(new AnyConcreteTypeNotAlreadyRegisteredSource());
            builder.RegisterType<ConsoleLogger>().As<ILogger>();

            builder.RegisterType<CacheManager>().As<ICacheManager>();
            builder.RegisterType<CacheStarter>().As<IStartable>();

            builder.RegisterType<OrderService>().As<IOrderService>()
                   .EnableClassInterceptors(ProxyGenerationOptions.Default, typeof(IOrderService))
                   .InterceptedBy(typeof(SecurityInterceptor), typeof(AuditInterceptor));

            builder.RegisterType<SecurityInterceptor>();
            builder.RegisterType<AuditInterceptor>();

            IContainer container = builder.Build();

            // COMPOSITION ROOT !

            var claims = Thread.CurrentPrincipal as ClaimsPrincipal;
            claims.AddIdentity(new ClaimsIdentity(new Claim[] { new Claim(ClaimTypes.Role, "User") }));

            // Resolution Root!
            var orderService = container.Resolve<IOrderService>();
            OrderResult result = orderService.DoOrder(1);

            Console.ReadKey();
        }
    }
}

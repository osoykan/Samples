using Autofac;

using DependencyInjectionCourse.Cache;
using DependencyInjectionCourse.ExternalDependencies;
using DependencyInjectionCourse.Order;

using FluentAssertions;

using NSubstitute;

using Xunit;

namespace DependencyInjectionCourse.Tests
{
    /// <summary>
    ///     Uses manual mocking and pure DI container, Attention! it is doing arrange inside Build Scope
    ///     for sake of readability.
    /// </summary>
    /// <seealso cref="DependencyInjectionCourse.Tests.TestBaseWithLocalIoc" />
    public class OrderService_Tests_2_With_DependencyInjection : TestBaseWithLocalIoc
    {
        [Fact]
        public void with_pure_dependency_injection()
        {
            //-----------------------------------------------------------------------------------------------------------
            // Arrange
            //-----------------------------------------------------------------------------------------------------------
            Building(builder =>
            {
                var fakeDepdency1 = Substitute.For<IDependency1>();
                var fakeDepdency2 = Substitute.For<IDependency2>();
                var fakeCacheManager = Substitute.For<ICacheManager>();
                fakeCacheManager.Get("1").Returns(new Basket(1, 50));

                builder.Register(context => fakeDepdency1);
                builder.Register(context => fakeDepdency2);
                builder.Register(context => fakeCacheManager);
                builder.RegisterType<OrderService>().As<IOrderService>();
            });

            var sut = Resolver.Resolve<IOrderService>();

            //-----------------------------------------------------------------------------------------------------------
            // Act
            //-----------------------------------------------------------------------------------------------------------
            OrderResult result = sut.DoOrder(1);

            //-----------------------------------------------------------------------------------------------------------
            // Assert
            //-----------------------------------------------------------------------------------------------------------
            result.BasketId.Should().Be(1);
            result.Total.Should().Be(50);
        }
    }
}

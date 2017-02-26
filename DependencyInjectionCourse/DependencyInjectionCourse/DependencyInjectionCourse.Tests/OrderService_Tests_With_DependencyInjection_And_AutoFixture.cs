using Autofac;

using DependencyInjectionCourse.Cache;
using DependencyInjectionCourse.ExternalDependencies;
using DependencyInjectionCourse.Order;
using DependencyInjectionCourse.Tests.Conventions;

using FluentAssertions;

using NSubstitute;

using Xunit;

namespace DependencyInjectionCourse.Tests
{
    public class OrderService_Tests_With_DependencyInjection_And_AutoFixture : TestBaseWithLocalIoc
    {
        [Theory]
        [AutoSubstitute]
        public void with_pure_dependency_injection_autofixture(
            IDependency1 fakeDependency1,
            IDependency2 fakeDependency2,
            ICacheManager fakeCacheManager
        )
        {
            //-----------------------------------------------------------------------------------------------------------
            // Arrange
            //-----------------------------------------------------------------------------------------------------------
            Building(builder =>
            {
                fakeCacheManager.Get("1").Returns(new Basket(1, 50));

                builder.Register(context => fakeDependency1);
                builder.Register(context => fakeDependency2);
                builder.Register(context => fakeCacheManager);
                builder.RegisterType<OrderService>().As<IOrderService>();
            }).Ok();

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

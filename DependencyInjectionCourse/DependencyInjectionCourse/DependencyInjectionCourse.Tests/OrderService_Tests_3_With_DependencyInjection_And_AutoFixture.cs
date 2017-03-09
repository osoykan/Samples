using Autofac;

using DependencyInjectionCourse.Cache;
using DependencyInjectionCourse.ExternalDependencies;
using DependencyInjectionCourse.Order;
using DependencyInjectionCourse.Tests.Conventions;

using FakeItEasy;

using FluentAssertions;

using Xunit;

namespace DependencyInjectionCourse.Tests
{
    /// <summary>
    ///     Uses pure Autofac Container and AutoFixture.
    /// </summary>
    /// <seealso cref="DependencyInjectionCourse.Tests.TestBaseWithLocalIoc" />
    public class OrderService_Tests_3_With_DependencyInjection_And_AutoFixture : TestBaseWithLocalIoc
    {
        [Theory]
        [AutoDataSubstitute]
        public void order_should_be_done_successfully(
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
                A.CallTo(() => fakeCacheManager.Get("1")).Returns(new Basket(1, 50));

                builder.Register(context => fakeDependency1);
                builder.Register(context => fakeDependency2);
                builder.Register(context => fakeCacheManager);
                builder.RegisterType<OrderService>().As<IOrderService>();
            });

            //-----------------------------------------------------------------------------------------------------------
            // Act
            //-----------------------------------------------------------------------------------------------------------
            var sut = The<IOrderService>();

            OrderResult result = sut.DoOrder(1);

            //-----------------------------------------------------------------------------------------------------------
            // Assert
            //-----------------------------------------------------------------------------------------------------------
            result.BasketId.Should().Be(1);
            result.Total.Should().Be(50);
            A.CallTo(() => fakeDependency1.Salute()).MustHaveHappened();
        }
    }
}

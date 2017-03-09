using Autofac;

using DependencyInjectionCourse.Cache;
using DependencyInjectionCourse.ExternalDependencies;
using DependencyInjectionCourse.Order;

using FakeItEasy;

using FluentAssertions;

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
        public void order_should_be_done_successfully()
        {
            //-----------------------------------------------------------------------------------------------------------
            // Arrange
            //-----------------------------------------------------------------------------------------------------------
            Building(builder =>
            {
                var fakeDepdency1 = A.Fake<IDependency1>();
                var fakeDepdency2 = A.Fake<IDependency2>();
                var fakeCacheManager = A.Fake<ICacheManager>();
                A.CallTo(() => fakeCacheManager.Get("1")).Returns(new Basket(1, 50));

                builder.Register(context => fakeDepdency1);
                builder.Register(context => fakeDepdency2);
                builder.Register(context => fakeCacheManager);
                builder.RegisterType<OrderService>().As<IOrderService>();
            });

            //-----------------------------------------------------------------------------------------------------------
            // Act
            //-----------------------------------------------------------------------------------------------------------

            // We get rid of the object creation which means object newing!
            var sut = The<IOrderService>();

            OrderResult result = sut.DoOrder(1);

            //-----------------------------------------------------------------------------------------------------------
            // Assert
            //-----------------------------------------------------------------------------------------------------------
            result.BasketId.Should().Be(1);
            result.Total.Should().Be(50);
            A.CallTo(() => The<IDependency1>().Salute()).MustHaveHappened();
        }
    }
}

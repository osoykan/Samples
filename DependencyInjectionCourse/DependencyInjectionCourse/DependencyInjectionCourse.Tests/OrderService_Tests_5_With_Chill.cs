using Chill;

using DependencyInjectionCourse.Cache;
using DependencyInjectionCourse.ExternalDependencies;
using DependencyInjectionCourse.Order;

using FakeItEasy;

using FluentAssertions;

using Xunit;

namespace DependencyInjectionCourse.Tests
{
    /// <summary>
    ///     This is another Container based approach!
    ///     Uses https://github.com/Erwinvandervalk/Chill
    /// </summary>
    public class OrderService_Tests_5_With_Chill : GivenSubject<OrderService, OrderResult>
    {
        public OrderService_Tests_5_With_Chill()
        {
            Given(() =>
            {
                SetThe<Basket>().To(new Basket(1, 50));
                A.CallTo(() => The<ICacheManager>().Get("1")).Returns(The<Basket>());
            });

            When(() => Subject.DoOrder(1));
        }

        [Fact]
        public void order_should_be_done_successfully()
        {
            Result.BasketId.Should().Be(1);
            Result.Total.Should().Be(50);
            A.CallTo(() => The<IDependency1>().Salute()).MustHaveHappened();
        }
    }
}

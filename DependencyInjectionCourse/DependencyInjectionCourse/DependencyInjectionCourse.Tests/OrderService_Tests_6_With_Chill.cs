using Chill;

using DependencyInjectionCourse.Cache;
using DependencyInjectionCourse.ExternalDependencies;
using DependencyInjectionCourse.Order;

using FluentAssertions;

using NSubstitute;

using Xunit;

namespace DependencyInjectionCourse.Tests
{
    /// <summary>
    ///     This is another Container based approach!
    ///     Uses https://github.com/Erwinvandervalk/Chill
    /// </summary>
    public class OrderService_Tests_6_With_Chill : GivenSubject<OrderService, OrderResult>
    {
        public OrderService_Tests_6_With_Chill()
        {
            Given(() =>
            {
                SetThe<Basket>().To(new Basket(1, 50));
                UseThe(Substitute.For<ICacheManager>()).Get("1").Returns(The<Basket>());
                UseThe(Substitute.For<IDependency1>());
                UseThe(Substitute.For<IDependency2>());
            });

            When(() => Subject.DoOrder(1));
        }

        [Fact]
        public void with_chill_behaviour_driven_design_as_scoped_with_single_operation_responsibility()
        {
            Result.BasketId.Should().Be(1);
            Result.Total.Should().Be(50);
        }
    }
}

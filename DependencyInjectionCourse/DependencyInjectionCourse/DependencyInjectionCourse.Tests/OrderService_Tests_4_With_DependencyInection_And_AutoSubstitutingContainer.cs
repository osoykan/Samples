using DependencyInjectionCourse.Cache;
using DependencyInjectionCourse.ExternalDependencies;
using DependencyInjectionCourse.Order;

using FakeItEasy;

using FluentAssertions;

using Xunit;

namespace DependencyInjectionCourse.Tests
{
    /// <summary>
    ///     Auto-Mocking container aspect <see cref="TestBaseWithAutoFakingIoc" />
    /// </summary>
    /// <seealso cref="DependencyInjectionCourse.Tests.TestBaseWithAutoFakingIoc" />
    public class OrderService_Tests_4_With_DependencyInection_And_AutoSubstitutingContainer : TestBaseWithAutoFakingIoc
    {
        [Fact]
        public void order_should_be_done_successfully()
        {
            //-----------------------------------------------------------------------------------------------------------
            // Arrange
            //-----------------------------------------------------------------------------------------------------------
            A.CallTo(() => Fake<ICacheManager>().Get("1")).Returns(new Basket(1, 50));

            //-----------------------------------------------------------------------------------------------------------
            // Act
            //-----------------------------------------------------------------------------------------------------------
            IOrderService sut = The<IOrderService, OrderService>();
            OrderResult result = sut.DoOrder(1);

            //-----------------------------------------------------------------------------------------------------------
            // Assert
            //-----------------------------------------------------------------------------------------------------------
            result.BasketId.Should().Be(1);
            result.Total.Should().Be(50);
            A.CallTo(() => Fake<IDependency1>().Salute()).MustHaveHappened();
        }
    }
}

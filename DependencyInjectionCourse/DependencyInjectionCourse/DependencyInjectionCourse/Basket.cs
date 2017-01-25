namespace DependencyInjectionCourse
{
    public class Basket
    {
        public Basket(int id, int total)
        {
            Id = id;
            Total = total;
        }

        public int Id { get; set; }

        public int Total { get; set; }
    }
}

namespace DependencyInjectionCourse
{
    public abstract class ResultBase
    {
        protected ResultBase() : this(true, string.Empty)
        {
        }

        protected ResultBase(bool isSucess, string message)
        {
            IsSucess = isSucess;
            Message = message;
        }

        public bool IsSucess { get; set; }

        public string Message { get; set; }
    }
}

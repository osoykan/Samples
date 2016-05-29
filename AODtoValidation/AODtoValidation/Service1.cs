namespace AODtoValidation
{
    #region using

    using System.ServiceProcess;

    #endregion

    internal partial class Service1 : ServiceBase
    {
        public Service1()
        {
            InitializeComponent();
        }

        public Bootstrapper Bootstrapper { get; set; }

        protected override void OnStart(string[] args)
        {
            Bootstrapper = new Bootstrapper();
            Bootstrapper.Start();
        }

        protected override void OnStop()
        {
            Bootstrapper.Dispose();
        }
    }
}